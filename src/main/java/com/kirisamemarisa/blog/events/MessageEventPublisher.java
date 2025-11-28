package com.kirisamemarisa.blog.events;

import com.kirisamemarisa.blog.dto.PrivateMessageDTO;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单的会话级 SSE 推送管理。
 * key = 排序后两个用户ID组合: min-max
 */
@Component
public class MessageEventPublisher {

    private final Map<String, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long meId, Long otherId, List<PrivateMessageDTO> initial) {
        String key = key(meId, otherId);
        SseEmitter emitter = new SseEmitter(0L); // 不超时
        emitters.computeIfAbsent(key, k -> Collections.synchronizedSet(new HashSet<>())).add(emitter);
        emitter.onCompletion(() -> remove(key, emitter));
        emitter.onTimeout(() -> remove(key, emitter));
        emitter.onError(e -> remove(key, emitter));
        try {
            emitter.send(SseEmitter.event().name("init").data(initial));
        } catch (IOException ignored) {}
        return emitter;
    }

    public void broadcast(Long aId, Long bId, List<PrivateMessageDTO> conversation) {
        String key = key(aId, bId);
        Set<SseEmitter> set = emitters.get(key);
        if (set == null) return;
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter em : set) {
            try {
                em.send(SseEmitter.event().name("update").data(conversation));
            } catch (Exception e) {
                dead.add(em);
            }
        }
        dead.forEach(d -> remove(key, d));
    }

    private void remove(String key, SseEmitter em) {
        Set<SseEmitter> set = emitters.get(key);
        if (set != null) {
            set.remove(em);
            if (set.isEmpty()) emitters.remove(key);
        }
    }

    private String key(Long a, Long b) {
        long min = Math.min(a, b);
        long max = Math.max(a, b);
        return min + "-" + max;
    }
}
