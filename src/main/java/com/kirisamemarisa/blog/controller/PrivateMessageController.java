package com.kirisamemarisa.blog.controller;

import com.kirisamemarisa.blog.common.ApiResponse;
import com.kirisamemarisa.blog.dto.PageResult;
import com.kirisamemarisa.blog.dto.PrivateMessageDTO;
import com.kirisamemarisa.blog.events.MessageEventPublisher;
import com.kirisamemarisa.blog.model.PrivateMessage;
import com.kirisamemarisa.blog.model.User;
import com.kirisamemarisa.blog.repository.UserRepository;
import com.kirisamemarisa.blog.service.PrivateMessageService;
import com.kirisamemarisa.blog.repository.PrivateMessageRepository;
import com.kirisamemarisa.blog.dto.ConversationSummaryDTO;
import com.kirisamemarisa.blog.model.UserProfile;
import com.kirisamemarisa.blog.repository.UserProfileRepository;
import com.kirisamemarisa.blog.service.NotificationService; // 新增导入
import com.kirisamemarisa.blog.dto.NotificationDTO; // 新增导入

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors; // 新增导入
import java.time.Instant; // 新增导入

@RestController
@RequestMapping("/api/messages")
public class PrivateMessageController {
    private final UserRepository userRepository;
    private final PrivateMessageService privateMessageService;
    private final PrivateMessageRepository privateMessageRepository;
    private final MessageEventPublisher publisher;
    private final UserProfileRepository userProfileRepository;
    private final NotificationService notificationService; // 新增字段

    // 私信媒体文件上传根目录（可在 application.yml/application.properties 中配置）
    @Value("${resource.message-media-location:uploads/messages}")
    private String messageMediaLocation;

    // 对外访问前缀，与静态资源映射一致，默认 `/files/messages`（不以 `/` 结尾）
    @Value("${resource.message-media-access-prefix:/files/messages}")
    private String messageMediaAccessPrefix;

    public PrivateMessageController(UserRepository userRepository,
                                    PrivateMessageService privateMessageService,
                                    PrivateMessageRepository privateMessageRepository,
                                    MessageEventPublisher publisher,
                                    UserProfileRepository userProfileRepository,
                                    NotificationService notificationService) { // 新增参数
        this.userRepository = userRepository;
        this.privateMessageService = privateMessageService;
        this.privateMessageRepository = privateMessageRepository;
        this.publisher = publisher;
        this.userProfileRepository = userProfileRepository;
        this.notificationService = notificationService; // 初始化新增字段
    }

    private User resolveCurrent(UserDetails principal, Long headerUserId) {
        if (principal != null)
            return userRepository.findByUsername(principal.getUsername());
        if (headerUserId != null)
            return userRepository.findById(headerUserId).orElse(null);
        return null;
    }

    private PrivateMessageDTO toDTO(PrivateMessage msg) {
        PrivateMessageDTO dto = new PrivateMessageDTO();
        dto.setId(msg.getId());
        dto.setSenderId(msg.getSender().getId());
        dto.setReceiverId(msg.getReceiver().getId());
        dto.setText(msg.getText());
        dto.setMediaUrl(msg.getMediaUrl());
        dto.setType(msg.getType());
        dto.setCreatedAt(msg.getCreatedAt());
        Long sid = msg.getSender() != null ? msg.getSender().getId() : null;
        Long rid = msg.getReceiver() != null ? msg.getReceiver().getId() : null;
        if (sid != null) {
            UserProfile sp = userProfileRepository.findById(sid).orElse(null);
            if (sp != null) {
                dto.setSenderNickname(sp.getNickname());
                dto.setSenderAvatarUrl(sp.getAvatarUrl());
            } else {
                dto.setSenderNickname(msg.getSender() != null ? msg.getSender().getUsername() : "");
                dto.setSenderAvatarUrl("");
            }
        }
        if (rid != null) {
            UserProfile rp = userProfileRepository.findById(rid).orElse(null);
            if (rp != null) {
                dto.setReceiverNickname(rp.getNickname());
                dto.setReceiverAvatarUrl(rp.getAvatarUrl());
            } else {
                dto.setReceiverNickname(msg.getReceiver() != null ? msg.getReceiver().getUsername() : "");
                dto.setReceiverAvatarUrl("");
            }
        }
        return dto;
    }

    private ApiResponse<PageResult<PrivateMessageDTO>> buildConversation(User me, User other, int page, int size) {
        List<PrivateMessageDTO> all = privateMessageService
                .conversation(me, other)
                .stream().map(this::toDTO).toList();
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        List<PrivateMessageDTO> pageList = all.subList(from, to);
        return new ApiResponse<>(200, "OK", new PageResult<>(pageList, all.size(), page, size));
    }

    @GetMapping("/conversation/{otherId}")
    public ApiResponse<PageResult<PrivateMessageDTO>> conversation(@PathVariable Long otherId,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size,
                                                                   @RequestHeader(name = "X-User-Id", required = false) Long headerUserId,
                                                                   @AuthenticationPrincipal UserDetails principal) {
        User me = resolveCurrent(principal, headerUserId);
        if (me == null)
            return new ApiResponse<>(401, "未认证", null);
        User other = userRepository.findById(otherId).orElse(null);
        if (other == null)
            return new ApiResponse<>(404, "用户不存在", null);
        return buildConversation(me, other, page, size);
    }

    @GetMapping("/conversation/list")
    public ApiResponse<PageResult<ConversationSummaryDTO>> listConversations(
            @RequestHeader(name = "X-User-Id", required = false) Long headerUserId,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        User me = resolveCurrent(principal, headerUserId);
        if (me == null)
            return new ApiResponse<>(401, "未认证", null);

        Map<Long, ConversationSummaryDTO> map = new LinkedHashMap<>();

        privateMessageRepository.findBySenderWithReceiverOrderByCreatedAtDesc(me).forEach(m -> {
            User receiver = m.getReceiver();
            Long otherId = receiver != null ? receiver.getId() : null;
            if (otherId == null) return;
            ConversationSummaryDTO cur = map.get(otherId);
            if (cur == null || m.getCreatedAt().isAfter(cur.getLastAt())) {
                ConversationSummaryDTO s = new ConversationSummaryDTO();
                s.setOtherId(otherId);
                com.kirisamemarisa.blog.model.UserProfile prof = userProfileRepository.findById(otherId).orElse(null);
                if (prof != null) { s.setNickname(prof.getNickname()); s.setAvatarUrl(prof.getAvatarUrl()); }
                else { s.setNickname(receiver.getUsername()); s.setAvatarUrl(""); }
                s.setLastMessage(choosePreview(m));
                s.setLastAt(m.getCreatedAt());
                map.put(otherId, s);
            }
        });

        privateMessageRepository.findByReceiverWithSenderOrderByCreatedAtDesc(me).forEach(m -> {
            User sender = m.getSender();
            Long otherId = sender != null ? sender.getId() : null;
            if (otherId == null) return;
            ConversationSummaryDTO cur = map.get(otherId);
            if (cur == null || m.getCreatedAt().isAfter(cur.getLastAt())) {
                ConversationSummaryDTO s = new ConversationSummaryDTO();
                com.kirisamemarisa.blog.model.UserProfile prof2 = userProfileRepository.findById(otherId).orElse(null);
                s.setOtherId(otherId);
                if (prof2 != null) { s.setNickname(prof2.getNickname()); s.setAvatarUrl(prof2.getAvatarUrl()); }
                else { s.setNickname(sender.getUsername()); s.setAvatarUrl(""); }
                s.setLastMessage(choosePreview(m));
                s.setLastAt(m.getCreatedAt());
                map.put(otherId, s);
            }
        });

        java.util.List<ConversationSummaryDTO> list = new java.util.ArrayList<>(map.values());
        list.forEach(s -> {
            long unread = privateMessageRepository.countUnreadBetween(me.getId(), s.getOtherId());
            s.setUnreadCount(unread);
        });
        Collections.sort(list, Comparator.comparing(ConversationSummaryDTO::getLastAt).reversed());
        PageResult<ConversationSummaryDTO> page = new PageResult<>(list, list.size(), 0, list.size());
        return new ApiResponse<>(200, "OK", page);
    }

    @GetMapping("/unread/total")
    public ApiResponse<Long> unreadTotal(
            @RequestHeader(name = "X-User-Id", required = false) Long headerUserId,
            @AuthenticationPrincipal UserDetails principal) {
        User me = resolveCurrent(principal, headerUserId);
        if (me == null) return new ApiResponse<>(401, "未认证", null);
        long total = privateMessageRepository.countUnreadTotal(me.getId());
        return new ApiResponse<>(200, "OK", total);
    }

    @PostMapping("/conversation/{otherId}/read")
    @Transactional
    public ApiResponse<Integer> markRead(@PathVariable Long otherId,
                                         @RequestHeader(name = "X-User-Id", required = false) Long headerUserId,
                                         @AuthenticationPrincipal UserDetails principal) {
        User me = resolveCurrent(principal, headerUserId);
        if (me == null) return new ApiResponse<>(401, "未认证", null);
        int updated = privateMessageRepository.markConversationRead(otherId, me.getId());
        return new ApiResponse<>(200, "OK", updated);
    }

    private String choosePreview(PrivateMessage m) {
        if (m.getType() != null && m.getType() != PrivateMessage.MessageType.TEXT) {
            // 非文本则直接用类型名称作为预览，比如 IMAGE / VIDEO
            String base = m.getType().name();
            String t = m.getText();
            if (t != null && !t.isEmpty()) {
                String cut = t.length() > 20 ? t.substring(0, 20) + "..." : t;
                return base + ":" + cut;
            }
            return base;
        }
        String t = m.getText();
        if (t == null)
            return "";
        return t.length() > 40 ? t.substring(0, 40) + "..." : t;
    }

    // 新增方法：发送私信通知
    private void sendPmNotification(PrivateMessage msg) {
        try {
            if (notificationService == null) {
                return;
            }
            NotificationDTO dto = new NotificationDTO();
            dto.setType("PRIVATE_MESSAGE");
            dto.setSenderId(msg.getSender() != null ? msg.getSender().getId() : null);
            dto.setReceiverId(msg.getReceiver() != null ? msg.getReceiver().getId() : null);
            dto.setMessage(choosePreview(msg)); // 调用自己的 choosePreview
            dto.setStatus(null);
            dto.setCreatedAt(Instant.now());

            Long receiverId = dto.getReceiverId();
            if (receiverId != null) {
                notificationService.sendNotification(receiverId, dto);
            }
        } catch (Exception ignored) {
            // 保持最小侵入：通知失败不影响主流程
        }
    }

    @PostMapping("/text/{otherId}")
    public ApiResponse<PrivateMessageDTO> sendText(@PathVariable Long otherId,
                                                   @RequestBody PrivateMessageDTO body,
                                                   @RequestHeader(name = "X-User-Id", required = false) Long headerUserId,
                                                   @AuthenticationPrincipal UserDetails principal) {
        User me = resolveCurrent(principal, headerUserId);
        if (me == null)
            return new ApiResponse<>(401, "未认证", null);
        User other = userRepository.findById(otherId).orElse(null);
        if (other == null)
            return new ApiResponse<>(404, "用户不存在", null);
        PrivateMessage msg = privateMessageService.sendText(me, other, body.getText());
        PrivateMessageDTO dto = toDTO(msg);
        publisher.broadcast(me.getId(), other.getId(),
                privateMessageService.conversation(me, other).stream().map(this::toDTO).collect(java.util.stream.Collectors.toList()));

        // 新增：发送通知
        sendPmNotification(msg);

        return new ApiResponse<>(200, "发送成功", dto);
    }

    @PostMapping("/media/{otherId}")
    public ApiResponse<PrivateMessageDTO> sendMedia(@PathVariable Long otherId,
                                                    @RequestBody PrivateMessageDTO body,
                                                    @RequestHeader(name = "X-User-Id", required = false) Long headerUserId,
                                                    @AuthenticationPrincipal UserDetails principal) {
        User me = resolveCurrent(principal, headerUserId);
        if (me == null)
            return new ApiResponse<>(401, "未认证", null);
        User other = userRepository.findById(otherId).orElse(null);
        if (other == null)
            return new ApiResponse<>(404, "用户不存在", null);
        PrivateMessage msg = privateMessageService.sendMedia(me, other, body.getType(), body.getMediaUrl(),
                body.getText());
        PrivateMessageDTO dto = toDTO(msg);
        publisher.broadcast(me.getId(), other.getId(),
                privateMessageService.conversation(me, other).stream().map(this::toDTO).collect(java.util.stream.Collectors.toList()));

        // 新增：发送通知
        sendPmNotification(msg);

        return new ApiResponse<>(200, "发送成功", dto);
    }

    // ===== 私信媒体文件保存工具方法 =====
    private String saveMessageMediaFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null) {
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < originalFilename.length() - 1) {
                ext = originalFilename.substring(dotIndex);
            }
        }

        // 生成唯一文件名，避免覆盖
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;

        // 确保上传目录存在，使用配置的 `messageMediaLocation`
        java.nio.file.Path dirPath = java.nio.file.Paths.get(messageMediaLocation).toAbsolutePath().normalize();
        try {
            java.nio.file.Files.createDirectories(dirPath);
        } catch (IOException e) {
            throw new IOException("创建上传目录失败: " + dirPath, e);
        }

        // 目标文件路径
        java.nio.file.Path destPath = dirPath.resolve(filename).normalize();
        try {
            file.transferTo(destPath.toFile());
        } catch (IOException e) {
            throw new IOException("保存上传文件失败: " + destPath, e);
        }

        // 规范化访问前缀：以 `/` 开头，不以 `/` 结尾
        String prefix = messageMediaAccessPrefix;
        if (prefix == null || prefix.isEmpty()) {
            prefix = "/files/messages";
        }
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }

        // 返回给前端的访问 URL，例如 `/files/messages/xxxxxx.jpg`
        return prefix + "/" + filename;
    }

    // ===== 私信媒体上传接口 =====
    @PostMapping("/upload")
    public ApiResponse<String> uploadMessageMedia(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(name = "X-User-Id", required = false) Long headerUserId,
            @AuthenticationPrincipal UserDetails principal) {

        User me = resolveCurrent(principal, headerUserId);
        if (me == null) {
            return new ApiResponse<>(401, "未认证", null);
        }

        if (file == null || file.isEmpty()) {
            return new ApiResponse<>(400, "上传文件不能为空", null);
        }

        String contentType = file.getContentType();
        if (contentType == null ||
                (!contentType.startsWith("image/") && !contentType.startsWith("video/"))) {
            return new ApiResponse<>(400, "仅支持图片或视频文件", null);
        }

        try {
            String url = saveMessageMediaFile(file);
            return new ApiResponse<>(200, "上传成功", url);
        } catch (IllegalArgumentException e) {
            return new ApiResponse<>(400, e.getMessage(), null);
        } catch (IOException e) {
            return new ApiResponse<>(500, "服务器保存文件失败", null);
        }
    }
}