package com.gittowork.global.exception;

public class WrongQuizTypeException extends RuntimeException {
    public WrongQuizTypeException(String message) {
        super(message);
    }
}
