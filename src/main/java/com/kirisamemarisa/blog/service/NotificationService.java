package com.kirisamemarisa.blog.service;

import com.kirisamemarisa.blog.dto.NotificationDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificationService {
    SseEmitter subscribe(Long userId, Object initialPayload);
    void sendNotification(Long userId, NotificationDTO payload);
    boolean isOnline(Long userId);
}
