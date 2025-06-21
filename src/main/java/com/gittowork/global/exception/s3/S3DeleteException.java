package com.gittowork.global.exception.s3;

public class S3DeleteException extends RuntimeException {
    public S3DeleteException(String message) {
        super(message);
    }
}
