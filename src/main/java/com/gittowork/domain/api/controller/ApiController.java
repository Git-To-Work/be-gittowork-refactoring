package com.gittowork.domain.api.controller;

import com.gittowork.domain.api.dto.response.ApiVersionResponse;
import com.gittowork.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/select/version")
    public ApiResponse<ApiVersionResponse> apiVersion() {
        ApiVersionResponse apiVersionResponse = ApiVersionResponse.builder()
                .version("0.0")
                .releaseDate("2025-03-19")
                .build();

        return ApiResponse.success(HttpStatus.OK, apiVersionResponse);
    }
}
