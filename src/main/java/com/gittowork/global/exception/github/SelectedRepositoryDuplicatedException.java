package com.gittowork.global.exception.github;

public class SelectedRepositoryDuplicatedException extends RuntimeException {
    public SelectedRepositoryDuplicatedException(String message) {
        super(message);
    }
}
