package com.kirisamemarisa.blog.service;

import com.kirisamemarisa.blog.dto.LoginResponseDTO;
import com.kirisamemarisa.blog.dto.UserLoginDTO;
import com.kirisamemarisa.blog.dto.UserRegisterDTO;
import com.kirisamemarisa.blog.dto.UserProfileDTO;

public interface UserService {
    void register(UserRegisterDTO dto);
    LoginResponseDTO login(UserLoginDTO dto);
    UserProfileDTO getUserProfileDTO(Long userId);
    boolean updateUserProfile(Long userId, UserProfileDTO dto);
    String getUsernameById(Long userId);
}
