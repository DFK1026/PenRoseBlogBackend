package com.kirisamemarisa.blog.mapper;

import com.kirisamemarisa.blog.dto.UserRegisterDTO;
import com.kirisamemarisa.blog.dto.UserLoginDTO;
import com.kirisamemarisa.blog.model.User;

public class UserMapper {
    public static User toUser(UserRegisterDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setNickname(dto.getNickname());
        user.setGender(dto.getGender());
        return user;
    }

    //没有啥必要
    public static User toUser(UserLoginDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        return user;
    }

    public static UserRegisterDTO toRegisterDTO(User user) {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername(user.getUsername());
        //密码不返回给前端
        dto.setNickname(user.getNickname());
        dto.setGender(user.getGender());
        return dto;
    }
}
