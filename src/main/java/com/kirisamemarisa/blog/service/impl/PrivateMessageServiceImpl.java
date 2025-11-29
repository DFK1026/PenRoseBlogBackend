package com.kirisamemarisa.blog.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(PrivateMessageServiceImpl.class);
    private final PrivateMessageRepository messageRepository;
    private final FollowService followService;

    public PrivateMessageServiceImpl(PrivateMessageRepository messageRepository, FollowService followService) {
        this.messageRepository = messageRepository;
        this.followService = followService;
        logger.debug("PrivateMessageServiceImpl initialized with messageRepository={} followService={}", messageRepository != null, followService != null);
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
    public PrivateMessage sendMedia(User sender, User receiver, PrivateMessage.MessageType type, String mediaUrl,
            String caption) {
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
        // use fetch-join queries so sender/receiver are initialized while the transaction/session is open
        List<PrivateMessage> ab = new ArrayList<>(messageRepository.findBySenderAndReceiverWithParticipantsOrderByCreatedAtAsc(a, b));
        List<PrivateMessage> ba = messageRepository.findBySenderAndReceiverWithParticipantsOrderByCreatedAtAsc(b, a);
        ab.addAll(ba);
        ab.sort(java.util.Comparator.comparing(PrivateMessage::getCreatedAt));
        return ab;
    }

    @Override
    public boolean canSendMedia(User sender, User receiver) {
        return followService.areFriends(sender, receiver) || hasReplied(sender, receiver);
    }

    @Override
    public boolean hasReplied(User sender, User receiver) {
        List<PrivateMessage> replies = messageRepository.findBySenderAndReceiverWithParticipantsOrderByCreatedAtAsc(receiver, sender);
        return !replies.isEmpty();
    }
}
