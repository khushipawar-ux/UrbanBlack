package com.urbanblack.userservice.service;

import com.urbanblack.userservice.dto.UserProfileRequest;
import com.urbanblack.userservice.dto.UserResponse;
import com.urbanblack.userservice.dto.UserUpdateRequest;

import java.util.List;

public interface UserService {

    UserResponse createOrGetProfile(Long userId, String emailHeader, UserProfileRequest request);

    UserResponse getMyProfile(Long userId);

    UserResponse getUserDetailsAdmin(Long userId);

    List<UserResponse> searchUsersAdmin(String mobile, String email);

    UserResponse updateMyProfile(Long userId, UserUpdateRequest request);

    void deactivateMyProfile(Long userId);

    long countUsers();
}
