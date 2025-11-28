package com.kirisamemarisa.blog.service.impl;

import com.kirisamemarisa.blog.common.BusinessException;
import com.kirisamemarisa.blog.common.JwtUtil;
import com.kirisamemarisa.blog.dto.LoginResponseDTO;
import com.kirisamemarisa.blog.dto.UserLoginDTO;
import com.kirisamemarisa.blog.dto.UserRegisterDTO;
import com.kirisamemarisa.blog.dto.UserProfileDTO;
import com.kirisamemarisa.blog.mapper.UserMapper;
import com.kirisamemarisa.blog.mapper.UserProfileMapper;
import com.kirisamemarisa.blog.model.User;
import com.kirisamemarisa.blog.model.UserProfile;
import com.kirisamemarisa.blog.repository.UserRepository;
import com.kirisamemarisa.blog.repository.UserProfileRepository;
import com.kirisamemarisa.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserProfileMapper userProfileMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void register(UserRegisterDTO dto) {
        if (dto == null) throw new BusinessException("请求体为空");
        String username = dto.getUsername();
        String password = dto.getPassword();
        if (username == null || !username.matches("^[A-Za-z0-9_]{5,15}$"))
            throw new BusinessException("用户名格式不合法");
        if (password == null || !password.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,12}$"))
            throw new BusinessException("密码格式不合法");
        if (userRepository.findByUsername(username) != null)
            throw new BusinessException("用户名已存在");
        User user = userMapper.toUser(dto);
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }

    @Override
    public LoginResponseDTO login(UserLoginDTO dto) {
        if (dto == null) throw new BusinessException("请求体为空");
        String username = dto.getUsername();
        String password = dto.getPassword();
        if (username == null || password == null)
            throw new BusinessException("用户名或密码为空");
        User dbUser = userRepository.findByUsername(username);
        if (dbUser == null) throw new BusinessException("用户不存在");
        if (!passwordEncoder.matches(password, dbUser.getPassword()))
            throw new BusinessException("密码错误");
        String token = JwtUtil.generateToken(dbUser.getId(), dbUser.getUsername());
        // 查询用户profile
        UserProfile profile = userProfileRepository.findById(dbUser.getId()).orElse(null);
        LoginResponseDTO resp = new LoginResponseDTO();
        resp.setToken(token);
        resp.setUserId(dbUser.getId());
        if (profile != null) {
            resp.setNickname(profile.getNickname());
            resp.setAvatarUrl(profile.getAvatarUrl());
            resp.setBackgroundUrl(profile.getBackgroundUrl());
            resp.setGender(profile.getGender());
        } else {
            resp.setNickname("");
            resp.setAvatarUrl("");
            resp.setBackgroundUrl("");
            resp.setGender(dbUser.getGender() != null ? dbUser.getGender() : "");
        }
        return resp;
    }

    @Override
    public UserProfileDTO getUserProfileDTO(Long userId) {
        if (userId == null) return null;
        Optional<UserProfile> opt = userProfileRepository.findById(userId);
        return opt.map(userProfileMapper::toDTO).orElse(null);
    }

    @Override
    public boolean updateUserProfile(Long userId, UserProfileDTO dto) {
        if (userId == null || dto == null) return false;
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return false;

        UserProfile profile = userProfileRepository.findById(userId).orElseGet(() -> {
            UserProfile p = new UserProfile();
            p.setUser(userOpt.get()); // MapsId 会同步主键
            return p;
        });

        profile.setNickname(dto.getNickname());
        profile.setAvatarUrl(dto.getAvatarUrl());
        profile.setBackgroundUrl(dto.getBackgroundUrl());

        userProfileRepository.save(profile);
        return true;
    }

    @Override
    public String getUsernameById(Long userId) {
        if (userId == null) return null;
        return userRepository.findById(userId).map(User::getUsername).orElse(null);
    }
}
