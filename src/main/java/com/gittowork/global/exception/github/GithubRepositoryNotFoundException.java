package com.gittowork.global.exception.github;

public class GithubRepositoryNotFoundException extends RuntimeException {
    public GithubRepositoryNotFoundException(String message) {
        super(message);
    }
}
