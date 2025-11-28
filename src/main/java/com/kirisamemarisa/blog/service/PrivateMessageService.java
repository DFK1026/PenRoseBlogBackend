package com.kirisamemarisa.blog.service;

import com.kirisamemarisa.blog.model.PrivateMessage;
import com.kirisamemarisa.blog.model.User;

import java.util.List;

public interface PrivateMessageService {
    PrivateMessage sendText(User sender, User receiver, String text);
    PrivateMessage sendMedia(User sender, User receiver, PrivateMessage.MessageType type, String mediaUrl, String caption);
    List<PrivateMessage> conversation(User a, User b);
    boolean canSendMedia(User sender, User receiver);
    boolean hasReplied(User sender, User receiver);
}
