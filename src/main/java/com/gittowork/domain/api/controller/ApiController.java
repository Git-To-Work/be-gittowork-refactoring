package com.gittowork.domain.api.controller;

import com.gittowork.domain.api.dto.response.ApiVersionResponse;
import com.gittowork.global.dto.response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Value("${release.version}")
    private String version;

    @Value("${release.date}")
    private String date;

    @GetMapping("/version")
    public ApiResponse<ApiVersionResponse> apiVersion() {
        ApiVersionResponse apiVersionResponse = ApiVersionResponse.builder()
                .version(version)
                .releaseDate(date)
                .build();

        return ApiResponse.success(HttpStatus.OK, apiVersionResponse);
    }

}
