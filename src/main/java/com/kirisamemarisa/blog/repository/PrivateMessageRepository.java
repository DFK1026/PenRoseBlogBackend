package com.kirisamemarisa.blog.repository;

import com.kirisamemarisa.blog.model.PrivateMessage;
import com.kirisamemarisa.blog.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, Long> {
    List<PrivateMessage> findBySenderAndReceiverOrderByCreatedAtAsc(User sender, User receiver);
    List<PrivateMessage> findByReceiverOrderByCreatedAtDesc(User receiver);
    List<PrivateMessage> findBySenderOrderByCreatedAtDesc(User sender);
}
