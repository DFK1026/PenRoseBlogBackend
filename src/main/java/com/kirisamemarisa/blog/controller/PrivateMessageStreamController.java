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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 私信会话实时流 (SSE)。
 */
@RestController
@RequestMapping("/api/messages")
public class PrivateMessageStreamController {

    private final UserRepository userRepository;
    private final PrivateMessageService privateMessageService;
    private final MessageEventPublisher publisher;

    public PrivateMessageStreamController(UserRepository userRepository,
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

    @GetMapping("/stream/{otherId}")
    public SseEmitter stream(@PathVariable Long otherId,
                             @RequestHeader(name = "X-User-Id", required = false) Long headerUserId,
                             @AuthenticationPrincipal UserDetails principal) {
        User me = resolveCurrent(principal, headerUserId);
        if (me == null) {
            // 返回一个立即结束的 emitter（前端识别失败回退轮询）
            SseEmitter failed = new SseEmitter();
            try {
                failed.send(SseEmitter.event().name("error")
                        .data(new ApiResponse<>(401, "未认证", null)));
            } catch (Exception ignored) {}
            failed.complete();
            return failed;
        }
        User other = userRepository.findById(otherId).orElse(null);
        if (other == null) {
            SseEmitter failed = new SseEmitter();
            try {
                failed.send(SseEmitter.event().name("error")
                        .data(new ApiResponse<>(404, "用户不存在", null)));
            } catch (Exception ignored) {}
            failed.complete();
            return failed;
        }
        List<PrivateMessageDTO> initial = privateMessageService
                .conversation(me, other)
                .stream().map(this::toDTO).collect(Collectors.toList());
        return publisher.subscribe(me.getId(), other.getId(), initial);
    }
}
