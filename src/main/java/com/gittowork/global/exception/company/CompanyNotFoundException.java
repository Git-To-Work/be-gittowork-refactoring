package com.gittowork.global.exception.company;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyNotFoundException extends RuntimeException {
    public CompanyNotFoundException(String message) {
        super(message);
    }
}
