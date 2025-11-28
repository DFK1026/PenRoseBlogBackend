package com.kirisamemarisa.blog.dto;

import com.kirisamemarisa.blog.model.PrivateMessage.MessageType;
import java.time.Instant;

public class PrivateMessageDTO {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String text;
    private String mediaUrl;
    private MessageType type;
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
