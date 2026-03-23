package com.urbanblack.userservice.service.impl;

import com.urbanblack.userservice.dto.UserProfileRequest;
import com.urbanblack.userservice.dto.UserResponse;
import com.urbanblack.userservice.dto.UserUpdateRequest;
import com.urbanblack.userservice.entity.User;
import com.urbanblack.userservice.exception.UserAlreadyExistsException;
import com.urbanblack.userservice.exception.UserNotFoundException;
import com.urbanblack.userservice.repository.UserRepository;
import com.urbanblack.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserResponse createOrGetProfile(Long userId, String emailHeader, UserProfileRequest request) {

        // Idempotent: if profile exists, return it
        User existing = userRepository.findById(userId).orElse(null);
        if (existing != null) {
            return mapToResponse(existing);
        }

        // Use email from request body if header is missing (OTP login case)
        String finalEmail = (emailHeader != null && !emailHeader.isEmpty() && !emailHeader.equals("null"))
                ? emailHeader
                : request.getEmail();

        if (finalEmail == null || finalEmail.isEmpty()) {
            throw new RuntimeException("Email is required for profile creation");
        }

        // Prevent duplicates by email/phone
        if (userRepository.existsByEmail(finalEmail)) {
            throw new UserAlreadyExistsException("Email already exists");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new UserAlreadyExistsException("Phone already exists");
        }

        User user = User.builder()
                .id(userId)
                .name(request.getName())
                .email(finalEmail)
                .phone(request.getPhone())
                .active(true)
                .build();

        return mapToResponse(userRepository.save(user));
    }

    @Override
    public UserResponse getMyProfile(Long userId) {
        return mapToResponse(findUser(userId));
    }

    @Override
    public UserResponse getUserDetailsAdmin(Long userId) {
        return mapToResponse(findUser(userId));
    }

    @Override
    public java.util.List<UserResponse> searchUsersAdmin(String mobile, String email) {
        return userRepository.findByEmailOrPhone(email, mobile).stream()
                .map(this::mapToResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional
    public UserResponse updateMyProfile(Long userId, UserUpdateRequest request) {
        User user = findUser(userId);

        if (!user.getPhone().equals(request.getPhone()) && userRepository.existsByPhone(request.getPhone())) {
            throw new UserAlreadyExistsException("Phone already exists");
        }

        user.setName(request.getName());
        user.setPhone(request.getPhone());

        return mapToResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deactivateMyProfile(Long userId) {
        User user = findUser(userId);
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    public long countUsers() {
        return userRepository.count();
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
