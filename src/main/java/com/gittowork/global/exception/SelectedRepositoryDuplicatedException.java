package com.gittowork.global.exception;

public class SelectedRepositoryDuplicatedException extends RuntimeException {
    public SelectedRepositoryDuplicatedException(String message) {
        super(message);
    }
}
