package com.kirisamemarisa.blog.controller;

import com.kirisamemarisa.blog.common.ApiResponse;
import com.kirisamemarisa.blog.dto.UserLoginDTO;
import com.kirisamemarisa.blog.dto.UserRegisterDTO;
import com.kirisamemarisa.blog.dto.UserProfileDTO;
import com.kirisamemarisa.blog.dto.LoginResponseDTO;
import com.kirisamemarisa.blog.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private UserService userService;

    // 注册接口
    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody @Valid UserRegisterDTO userRegisterDTO) {
        userService.register(userRegisterDTO);
        return new ApiResponse<>(200, "注册成功", null);
    }

    // 登录接口
    @PostMapping("/login")
    public ApiResponse<LoginResponseDTO> login(@RequestBody @Valid UserLoginDTO userLoginDTO) {
        LoginResponseDTO resp = userService.login(userLoginDTO);
        return new ApiResponse<>(200, "登录成功", resp);
    }

    // 获取用户个人信息
    @GetMapping("/profile/{userId}")
    public ApiResponse<UserProfileDTO> getProfile(@PathVariable Long userId) {
        UserProfileDTO dto = userService.getUserProfileDTO(userId);
        if (dto == null) {
            return new ApiResponse<>(404, "用户信息不存在", null);
        }
        return new ApiResponse<>(200, "获取成功", dto);
    }

    // 更新用户个人信息
    @PutMapping("/profile/{userId}")
    public ApiResponse<Void> updateProfile(@PathVariable Long userId,
                                           @RequestBody UserProfileDTO userProfileDTO,
                                           @AuthenticationPrincipal UserDetails principal) {
        if (principal == null || !principal.getUsername().equals(userService.getUsernameById(userId))) {
            return new ApiResponse<>(403, "无权修改他人资料", null);
        }
        boolean ok = userService.updateUserProfile(userId, userProfileDTO);
        return ok ? new ApiResponse<>(200, "更新成功", null)
                : new ApiResponse<>(400, "更新失败", null);
    }

}
