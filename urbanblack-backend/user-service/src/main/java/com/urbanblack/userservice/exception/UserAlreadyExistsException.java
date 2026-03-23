package com.urbanblack.userservice.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserAlreadyExistsException extends RuntimeException {
    private String message;
    private HttpStatus status = HttpStatus.CONFLICT;

    public UserAlreadyExistsException(String message) {
        super(message);
        this.message = message;
    }
}