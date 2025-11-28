package com.kirisamemarisa.blog.service.impl;

import com.kirisamemarisa.blog.model.PrivateMessage;
import com.kirisamemarisa.blog.model.User;
import com.kirisamemarisa.blog.repository.PrivateMessageRepository;
import com.kirisamemarisa.blog.service.FollowService;
import com.kirisamemarisa.blog.service.PrivateMessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class PrivateMessageServiceImpl implements PrivateMessageService {
    private final PrivateMessageRepository messageRepository;
    private final FollowService followService;

    public PrivateMessageServiceImpl(PrivateMessageRepository messageRepository, FollowService followService) {
        this.messageRepository = messageRepository;
        this.followService = followService;
    }

    @Override
    public PrivateMessage sendText(User sender, User receiver, String text) {
        PrivateMessage msg = new PrivateMessage();
        msg.setSender(sender);
        msg.setReceiver(receiver);
        msg.setText(text);
        msg.setType(PrivateMessage.MessageType.TEXT);

        List<PrivateMessage> history = conversation(sender, receiver);
        boolean isFriend = followService.areFriends(sender, receiver);
        boolean replied = hasReplied(sender, receiver);

        if (!isFriend && !replied) {
            boolean hasSentBefore = history.stream().anyMatch(m -> m.getSender().equals(sender));
            if (hasSentBefore) {
                throw new IllegalStateException("非好友关系下仅允许发送一条文本，等待对方回复后才能继续。");
            }
        }
        return messageRepository.save(msg);
    }

    @Override
    public PrivateMessage sendMedia(User sender, User receiver, PrivateMessage.MessageType type, String mediaUrl, String caption) {
        if (!canSendMedia(sender, receiver)) {
            throw new IllegalStateException("发送媒体需互相关注或对方已回复。");
        }
        PrivateMessage msg = new PrivateMessage();
        msg.setSender(sender);
        msg.setReceiver(receiver);
        msg.setType(type);
        msg.setMediaUrl(mediaUrl);
        msg.setText(caption);
        return messageRepository.save(msg);
    }

    @Override
    public List<PrivateMessage> conversation(User a, User b) {
        List<PrivateMessage> ab = new ArrayList<>(messageRepository.findBySenderAndReceiverOrderByCreatedAtAsc(a, b));
        List<PrivateMessage> ba = messageRepository.findBySenderAndReceiverOrderByCreatedAtAsc(b, a);
        ab.addAll(ba);
        ab.sort((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt()));
        return ab;
    }

    @Override
    public boolean canSendMedia(User sender, User receiver) {
        return followService.areFriends(sender, receiver) || hasReplied(sender, receiver);
    }

    @Override
    public boolean hasReplied(User sender, User receiver) {
        return !messageRepository.findBySenderAndReceiverOrderByCreatedAtAsc(receiver, sender).isEmpty();
    }
}
