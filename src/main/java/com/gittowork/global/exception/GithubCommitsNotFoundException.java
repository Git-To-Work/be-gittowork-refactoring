package com.gittowork.global.exception;

public class GithubCommitsNotFoundException extends RuntimeException {
    public GithubCommitsNotFoundException(String message) {
        super(message);
    }
}
