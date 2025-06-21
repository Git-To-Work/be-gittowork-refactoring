package com.gittowork.global.exception.github;

public class GithubCommitsNotFoundException extends RuntimeException {
    public GithubCommitsNotFoundException(String message) {
        super(message);
    }
}
