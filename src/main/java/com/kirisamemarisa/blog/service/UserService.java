package com.kirisamemarisa.blog.service;

import com.kirisamemarisa.blog.common.ApiResponse;
import com.kirisamemarisa.blog.dto.UserLoginDTO;
import com.kirisamemarisa.blog.dto.UserRegisterDTO;
import com.kirisamemarisa.blog.mapper.UserMapper;
import com.kirisamemarisa.blog.model.User;
import com.kirisamemarisa.blog.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public ApiResponse<Void> register(UserRegisterDTO userRegisterDTO) {
        String username = userRegisterDTO.getUsername();
        String password = userRegisterDTO.getPassword();
        // 用户名校验
        if (username == null || !username.matches("^[A-Za-z0-9_]{5,15}$")) {
            return new ApiResponse<Void>(400, "用户名必须为5-15位，仅支持数字、字母、下划线", null);
        }
        // 密码校验
        if (password == null || !password.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,12}$")) {
            return new ApiResponse<Void>(400, "密码必须为8-12位，且包含数字和字母，不允许其他字符", null);
        }
        if (userRepository.findByUsername(username) != null) {
            return new ApiResponse<Void>(400, "用户名已存在", null);
        }
        User user = UserMapper.toUser(userRegisterDTO);
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        return new ApiResponse<Void>(200, "注册成功", null);
    }

    public ApiResponse<Void> login(UserLoginDTO userLoginDTO) {
        User dbUser = userRepository.findByUsername(userLoginDTO.getUsername());
        if (dbUser == null) {
            return new ApiResponse<Void>(400, "用户不存在", null);
        }
        if (!passwordEncoder.matches(userLoginDTO.getPassword(), dbUser.getPassword())) {
            return new ApiResponse<Void>(400, "密码错误", null);
        }
        return new ApiResponse<Void>(200, "登录成功", null);
    }
}
