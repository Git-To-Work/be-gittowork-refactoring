package com.gittowork.global.response;

import lombok.*;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private static final String DEFAULT_SUCCESS_MESSAGE = "Success";

    private int status;
    private String code;
    private String message;
    private T results;

    public static <T> ApiResponse<T> success(T results) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.OK.value())
                .code(String.valueOf(HttpStatus.OK.value()))
                .message(DEFAULT_SUCCESS_MESSAGE)
                .results(results)
                .build();
    }

    public static <T> ApiResponse<T> success(HttpStatus status, T results) {
        return ApiResponse.<T>builder()
                .status(status.value())
                .code(String.valueOf(status.value()))
                .message(DEFAULT_SUCCESS_MESSAGE)
                .results(results)
                .build();
    }
}
