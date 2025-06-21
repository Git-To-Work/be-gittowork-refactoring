package com.gittowork.global.exception.github;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GithubSignInException extends RuntimeException {
    public GithubSignInException(String message) {
        super(message);
    }
}
