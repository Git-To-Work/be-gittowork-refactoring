package com.gittowork.global.exception.auth;

public class AccessTokenNotFoundException extends RuntimeException {
    public AccessTokenNotFoundException(String message) {
        super(message);
    }
}
