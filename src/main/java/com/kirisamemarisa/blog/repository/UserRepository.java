package com.kirisamemarisa.blog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.kirisamemarisa.blog.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
}
