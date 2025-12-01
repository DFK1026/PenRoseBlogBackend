//java
//撤回：确认当前用户是 sender，然后在所有相关 PrivateMessageStatus 上标记 recalled = true。
//删除：仅在当前用户自己的 PrivateMessageStatus 上标记 deletedForUser = true。
//获取会话时：先用原来的 PrivateMessageService.conversation(...) 拿到所有消息，再根据 PrivateMessageStatus 过滤（对当前用户已删除的剔除），并生成视图 DTO

package com.kirisamemarisa.blog.service.impl;

import com.kirisamemarisa.blog.dto.PrivateMessageViewDTO;
import com.kirisamemarisa.blog.mapper.PrivateMessageViewMapper;
import com.kirisamemarisa.blog.model.PrivateMessage;
import com.kirisamemarisa.blog.model.PrivateMessageStatus;
import com.kirisamemarisa.blog.model.User;
import com.kirisamemarisa.blog.repository.PrivateMessageRepository;
import com.kirisamemarisa.blog.repository.PrivateMessageStatusRepository;
import com.kirisamemarisa.blog.service.PrivateMessageManageService;
import com.kirisamemarisa.blog.service.PrivateMessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PrivateMessageManageServiceImpl implements PrivateMessageManageService {

    private static final long RECALL_LIMIT_MINUTES = 2L;

    private final PrivateMessageRepository messageRepository;
    private final PrivateMessageStatusRepository statusRepository;
    private final PrivateMessageService privateMessageService;

    public PrivateMessageManageServiceImpl(PrivateMessageRepository messageRepository,
                                           PrivateMessageStatusRepository statusRepository,
                                           PrivateMessageService privateMessageService) {
        this.messageRepository = messageRepository;
        this.statusRepository = statusRepository;
        this.privateMessageService = privateMessageService;
    }

    @Override
    public void recallMessage(User currentUser, Long messageId) {
        PrivateMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("消息不存在"));

        // 只能撤回自己发的
        if (message.getSender() == null
                || !Objects.equals(message.getSender().getId(), currentUser.getId())) {
            throw new IllegalStateException("只能撤回自己发送的消息");
        }

        // 时间限制：发送两分钟内才允许撤回
        Instant createdAt = message.getCreatedAt();
        if (createdAt != null) {
            long minutes = Duration.between(createdAt, Instant.now()).toMinutes();
            if (minutes > RECALL_LIMIT_MINUTES) {
                throw new IllegalStateException("消息发出超过两分钟");
            }
        } else {
            // 如果 createdAt 为空，为了安全起见视为不可撤回
            throw new IllegalStateException("消息发出超过两分钟");
        }

        // 找到所有与该消息相关的状态记录（包括发送方和接收方）
        List<PrivateMessageStatus> allStatus = statusRepository.findByMessage(message);
        if (allStatus.isEmpty()) {
            // 如之前未建立状态记录，则针对 sender & receiver 补一条
            List<User> users = new ArrayList<>();
            if (message.getSender() != null) users.add(message.getSender());
            if (message.getReceiver() != null) users.add(message.getReceiver());
            for (User u : users) {
                PrivateMessageStatus s = new PrivateMessageStatus();
                s.setMessage(message);
                s.setUser(u);
                s.setRecalled(true);
                s.setDeletedForUser(false);
                statusRepository.save(s);
            }
        } else {
            for (PrivateMessageStatus s : allStatus) {
                s.setRecalled(true);
            }
            statusRepository.saveAll(allStatus);
        }
    }

    @Override
    public void deleteMessageForUser(User currentUser, Long messageId) {
        PrivateMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("消息不存在"));

        PrivateMessageStatus status = statusRepository
                .findByMessageAndUser(message, currentUser)
                .orElseGet(() -> {
                    PrivateMessageStatus s = new PrivateMessageStatus();
                    s.setMessage(message);
                    s.setUser(currentUser);
                    return s;
                });

        status.setDeletedForUser(true);
        statusRepository.save(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrivateMessageViewDTO> getConversationView(User currentUser, User otherUser) {
        // 复用原有的会话查询逻辑
        List<PrivateMessage> raw = privateMessageService.conversation(currentUser, otherUser);
        if (raw.isEmpty()) return Collections.emptyList();

        // 批量拉取该用户对应的状态
        List<PrivateMessageStatus> statusList = statusRepository.findByMessageInAndUser(raw, currentUser);
        Map<Long, PrivateMessageStatus> statusMap = statusList.stream()
                .filter(s -> s.getMessage() != null && s.getMessage().getId() != null)
                .collect(Collectors.toMap(s -> s.getMessage().getId(), s -> s));

        List<PrivateMessageViewDTO> result = new ArrayList<>();
        for (PrivateMessage m : raw) {
            PrivateMessageStatus st = statusMap.get(m.getId());
            if (st != null && st.isDeletedForUser()) {
                // 当前用户本地删除的消息不再展示
                continue;
            }
            PrivateMessageViewDTO dto = PrivateMessageViewMapper.toViewDTO(m, st, currentUser);
            result.add(dto);
        }
        return result;
    }
}
