package com.kirisamemarisa.blog.service.impl;

import com.kirisamemarisa.blog.model.Follow;
import com.kirisamemarisa.blog.model.User;
import com.kirisamemarisa.blog.model.UserProfile;
import com.kirisamemarisa.blog.repository.FollowRepository;
import com.kirisamemarisa.blog.repository.UserRepository;
import com.kirisamemarisa.blog.service.FollowService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class FollowServiceImpl implements FollowService {
    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public FollowServiceImpl(FollowRepository followRepository, UserRepository userRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Follow follow(User follower, User followee) {
        return followRepository.findByFollowerAndFollowee(follower, followee)
                .orElseGet(() -> {
                    Follow f = new Follow();
                    f.setFollower(follower);
                    f.setFollowee(followee);
                    return followRepository.save(f);
                });
    }

    @Override
    public void unfollow(User follower, User followee) {
        followRepository.findByFollowerAndFollowee(follower, followee)
                .ifPresent(followRepository::delete);
    }

    @Override
    public boolean isFollowing(User follower, User followee) {
        return followRepository.findByFollowerAndFollowee(follower, followee).isPresent();
    }

    @Override
    public boolean areFriends(User a, User b) {
        return isFollowing(a, b) && isFollowing(b, a);
    }

    @Override
    public List<User> listFollowers(User user) {
        return followRepository.findByFollowee(user).stream()
                .map(Follow::getFollower)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> listFollowing(User user) {
        return followRepository.findByFollower(user).stream()
                .map(Follow::getFollowee)
                .collect(Collectors.toList());
    }

    @Override
    public List<Object[]> pageFollowers(User user, Pageable pageable) {
        return followRepository.findFollowersWithProfile(user, pageable);
    }

    @Override
    public List<Object[]> pageFollowing(User user, Pageable pageable) {
        return followRepository.findFollowingWithProfile(user, pageable);
    }

    @Override
    public long countFollowers(User user) {
        return followRepository.countByFollowee(user);
    }

    @Override
    public long countFollowing(User user) {
        return followRepository.countByFollower(user);
    }
}
