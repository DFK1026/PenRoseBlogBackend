package com.kirisamemarisa.blog.controller;

import com.kirisamemarisa.blog.common.ApiResponse;
import com.kirisamemarisa.blog.dto.PrivateMessageDTO;
import com.kirisamemarisa.blog.events.MessageEventPublisher;
import com.kirisamemarisa.blog.model.PrivateMessage;
import com.kirisamemarisa.blog.model.User;
import com.kirisamemarisa.blog.repository.UserRepository;
import com.kirisamemarisa.blog.service.PrivateMessageService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 私信发送与会话查询（增加未认证头兼容 + 推送事件）。
 */
@RestController
@RequestMapping("/api/messages")
public class PrivateMessageController {

    private final UserRepository userRepository;
    private final PrivateMessageService privateMessageService;
    private final MessageEventPublisher publisher;

    public PrivateMessageController(UserRepository userRepository,
                                    PrivateMessageService privateMessageService,
                                    MessageEventPublisher publisher) {
        this.userRepository = userRepository;
        this.privateMessageService = privateMessageService;
        this.publisher = publisher;
    }

    private User resolveCurrent(UserDetails principal, Long headerUserId) {
        if (principal != null) return userRepository.findByUsername(principal.getUsername());
        if (headerUserId != null) return userRepository.findById(headerUserId).orElse(null);
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
        return dto;
    }

    private ApiResponse<List<PrivateMessageDTO>> buildConversation(User me, User other) {
        List<PrivateMessageDTO> list = privateMessageService
                .conversation(me, other)
                .stream().map(this::toDTO).collect(Collectors.toList());
        return new ApiResponse<>(200, "OK", list);
    }

    @GetMapping("/conversation/{otherId}")
    public ApiResponse<List<PrivateMessageDTO>> conversation(@PathVariable Long otherId,
                                                             @RequestHeader(name = "X-User-Id", required = false) Long headerUserId,
                                                             @AuthenticationPrincipal UserDetails principal) {
        User me = resolveCurrent(principal, headerUserId);
        if (me == null) return new ApiResponse<>(401, "未认证", null);
        User other = userRepository.findById(otherId).orElse(null);
        if (other == null) return new ApiResponse<>(404, "用户不存在", null);
        return buildConversation(me, other);
    }

    @PostMapping("/text/{otherId}")
    public ApiResponse<PrivateMessageDTO> sendText(@PathVariable Long otherId,
                                                   @RequestBody PrivateMessageDTO body,
                                                   @RequestHeader(name = "X-User-Id", required = false) Long headerUserId,
                                                   @AuthenticationPrincipal UserDetails principal) {
        User me = resolveCurrent(principal, headerUserId);
        if (me == null) return new ApiResponse<>(401, "未认证", null);
        User other = userRepository.findById(otherId).orElse(null);
        if (other == null) return new ApiResponse<>(404, "用户不存在", null);
        PrivateMessage msg = privateMessageService.sendText(me, other, body.getText());
        PrivateMessageDTO dto = toDTO(msg);
        // 推送最新会话
        publisher.broadcast(me.getId(), other.getId(),
                privateMessageService.conversation(me, other).stream().map(this::toDTO).collect(Collectors.toList()));
        return new ApiResponse<>(200, "发送成功", dto);
    }

    @PostMapping("/media/{otherId}")
    public ApiResponse<PrivateMessageDTO> sendMedia(@PathVariable Long otherId,
                                                    @RequestBody PrivateMessageDTO body,
                                                    @RequestHeader(name = "X-User-Id", required = false) Long headerUserId,
                                                    @AuthenticationPrincipal UserDetails principal) {
        User me = resolveCurrent(principal, headerUserId);
        if (me == null) return new ApiResponse<>(401, "未认证", null);
        User other = userRepository.findById(otherId).orElse(null);
        if (other == null) return new ApiResponse<>(404, "用户不存在", null);
        PrivateMessage msg = privateMessageService.sendMedia(me, other, body.getType(), body.getMediaUrl(), body.getText());
        PrivateMessageDTO dto = toDTO(msg);
        publisher.broadcast(me.getId(), other.getId(),
                privateMessageService.conversation(me, other).stream().map(this::toDTO).collect(Collectors.toList()));
        return new ApiResponse<>(200, "发送成功", dto);
    }
}
