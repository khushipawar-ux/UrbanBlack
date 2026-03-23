package com.urbanblack.userservice.controller;

import com.urbanblack.common.dto.response.ApiResponse;
import com.urbanblack.userservice.context.UserContext;
import com.urbanblack.userservice.dto.UserProfileRequest;
import com.urbanblack.userservice.dto.UserResponse;
import com.urbanblack.userservice.dto.UserUpdateRequest;
import com.urbanblack.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> createProfile(@Valid @RequestBody UserProfileRequest request) {

        Long userId = UserContext.getUserId();
        String email = UserContext.getEmail();
        String role = UserContext.getRole();

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<UserResponse>builder()
                            .success(false)
                            .message("Missing authenticated user ID context")
                            .build());
        }

        // Optional: strict role check
        // if (!"USER".equals(role)) { ... }

        UserResponse response = userService.createOrGetProfile(userId, email, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<UserResponse>builder()
                        .success(true)
                        .message("User profile created (or retrieved)")
                        .data(response)
                        .build());
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<UserResponse>builder()
                            .success(false)
                            .message("Missing X-User-Id")
                            .build());
        }

        UserResponse response = userService.getMyProfile(userId);

        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .message("User profile fetched successfully")
                        .data(response)
                        .build());
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(@Valid @RequestBody UserUpdateRequest request) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<UserResponse>builder()
                            .success(false)
                            .message("Missing X-User-Id")
                            .build());
        }

        UserResponse response = userService.updateMyProfile(userId, request);

        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .message("User profile updated successfully")
                        .data(response)
                        .build());
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deactivateMyProfile() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Missing X-User-Id")
                            .build());
        }

        userService.deactivateMyProfile(userId);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("User deactivated successfully")
                        .data(null)
                        .build());
    }

    /**
     * Internal endpoint for service-to-service user lookups (e.g. ride-service fetching passenger name).
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable("userId") Long userId) {
        try {
            UserResponse response = userService.getMyProfile(userId);
            return ResponseEntity.ok(
                    ApiResponse.<UserResponse>builder()
                            .success(true)
                            .message("User fetched")
                            .data(response)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<UserResponse>builder()
                            .success(false)
                            .message("User not found")
                            .build());
        }
    }

    // ADMIN ENDPOINTS
    @GetMapping("/admin/users/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getAdminUserDetails(@PathVariable Long userId) {
        UserResponse response = userService.getUserDetailsAdmin(userId);
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .data(response)
                .build());
    }

    @GetMapping("/admin/users")
    public ResponseEntity<ApiResponse<java.util.List<UserResponse>>> searchUsers(
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) String email) {
        java.util.List<UserResponse> users = userService.searchUsersAdmin(mobile, email);
        return ResponseEntity.ok(ApiResponse.<java.util.List<UserResponse>>builder()
                .success(true)
                .data(users)
                .build());
    }

    @GetMapping("/admin/analytics/users/total")
    public ResponseEntity<java.util.Map<String, Long>> getTotalUsers() {
        long total = userService.countUsers();
        return ResponseEntity.ok(java.util.Map.of("totalUsers", total));
    }
}
