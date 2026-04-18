package com.aipostman.service;

import com.aipostman.domain.User;
import com.aipostman.dto.request.CreateUserRequest;
import com.aipostman.dto.request.UpdateUserRequest;
import com.aipostman.dto.response.UserResponse;
import com.aipostman.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        return userRepository.findByEmail(request.email())
                .map(existing -> new UserResponse(
                        existing.getId(),
                        existing.getEmail(),
                        existing.getDisplayName(),
                        existing.getTimezone()))
                .orElseGet(() -> createNewUser(request));
    }

    private UserResponse createNewUser(CreateUserRequest request) {
        User user = new User();
        user.setEmail(request.email());
        user.setDisplayName(request.displayName());
        user.setTimezone(request.timezone() == null || request.timezone().isBlank() ? "Asia/Shanghai" : request.timezone());
        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    @Transactional
    public UserResponse update(Long userId, UpdateUserRequest request) {
        User user = getUser(userId);
        userRepository.findByEmail(request.email())
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Email already exists: " + request.email());
                });
        user.setEmail(request.email());
        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }
        if (request.timezone() != null && !request.timezone().isBlank()) {
            user.setTimezone(request.timezone());
        }
        return toResponse(userRepository.save(user));
    }

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    public UserResponse get(Long userId) {
        return toResponse(getUser(userId));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getTimezone());
    }
}
