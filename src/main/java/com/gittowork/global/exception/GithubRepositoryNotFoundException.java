package com.gittowork.global.exception;

public class GithubRepositoryNotFoundException extends RuntimeException {
    public GithubRepositoryNotFoundException(String message) {
        super(message);
    }
}
