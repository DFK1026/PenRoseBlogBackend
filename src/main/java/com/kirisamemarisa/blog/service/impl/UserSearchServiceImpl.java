package com.kirisamemarisa.blog.service.impl;

import com.kirisamemarisa.blog.model.User;
import com.kirisamemarisa.blog.model.UserProfile;
import com.kirisamemarisa.blog.repository.UserRepository;
import com.kirisamemarisa.blog.service.UserSearchService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserSearchServiceImpl implements UserSearchService {
    private final UserRepository userRepository;

    public UserSearchServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<Object[]> searchByUsernameWithProfile(String username, int page, int size) {
        return userRepository.searchByUsernameWithProfile(username, PageRequest.of(page, size));
    }

    @Override
    public List<Object[]> searchByNicknameWithProfile(String nickname, int page, int size) {
        return userRepository.searchByNicknameWithProfile(nickname, PageRequest.of(page, size));
    }

    @Override
    public long countByUsername(String username) {
        return userRepository.countByUsername(username);
    }

    @Override
    public long countByNickname(String nickname) {
        return userRepository.countByNickname(nickname);
    }
}
