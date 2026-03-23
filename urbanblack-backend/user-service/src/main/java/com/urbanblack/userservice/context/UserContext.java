package com.urbanblack.userservice.context;

public class UserContext {

    private static final ThreadLocal<Long> currentUserId = new ThreadLocal<>();
    private static final ThreadLocal<String> currentEmail = new ThreadLocal<>();
    private static final ThreadLocal<String> currentRole = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        currentUserId.set(userId);
    }

    public static Long getUserId() {
        return currentUserId.get();
    }

    public static void setEmail(String email) {
        currentEmail.set(email);
    }

    public static String getEmail() {
        return currentEmail.get();
    }

    public static void setRole(String role) {
        currentRole.set(role);
    }

    public static String getRole() {
        return currentRole.get();
    }

    public static void clear() {
        currentUserId.remove();
        currentEmail.remove();
        currentRole.remove();
    }
}
