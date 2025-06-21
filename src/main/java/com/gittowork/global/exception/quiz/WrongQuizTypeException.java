package com.gittowork.global.exception.quiz;

public class WrongQuizTypeException extends RuntimeException {
    public WrongQuizTypeException(String message) {
        super(message);
    }
}
