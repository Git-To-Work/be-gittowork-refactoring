package com.gittowork.domain.field.controller;

import com.gittowork.domain.field.dto.response.GetInterestFieldsResponse;
import com.gittowork.domain.field.service.FieldService;
import com.gittowork.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관심 분야 목록 조회를 위한 REST API 컨트롤러
 */
@RestController
@RequestMapping(value = "/fields", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Field", description = "관심 분야 관리 API")
@RequiredArgsConstructor
public class FieldController {

    private final FieldService fieldService;

    @Operation(summary = "관심 분야 목록 조회", description = "사용자의 관심 분야 리스트를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetInterestFieldsResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @GetMapping
    public ApiResponse<GetInterestFieldsResponse> getInterestFields() {
        return ApiResponse.success(fieldService.getInterestFields());
    }
}
