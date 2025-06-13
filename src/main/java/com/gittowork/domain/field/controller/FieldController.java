package com.gittowork.domain.field.controller;

import com.gittowork.domain.field.service.FieldService;
import com.gittowork.domain.field.dto.response.GetInterestFieldsResponse;
import com.gittowork.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/fields")
public class FieldController {

    private final FieldService fieldService;

    @Operation(summary = "Get Interest Field List", description = "관심분야 리스트 조회")
    @GetMapping
    public ApiResponse<GetInterestFieldsResponse> getInterestFields() {
        return ApiResponse.success(HttpStatus.OK, fieldService.getInterestFields());
    }
}
