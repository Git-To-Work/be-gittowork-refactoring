package com.gittowork.global.exception;

public class AccessTokenNotFoundException extends RuntimeException {
    public AccessTokenNotFoundException(String message) {
        super(message);
    }
}
