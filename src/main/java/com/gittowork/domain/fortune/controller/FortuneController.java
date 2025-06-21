package com.gittowork.domain.fortune.controller;

import com.gittowork.domain.fortune.dto.request.GetTodayFortuneRequest;
import com.gittowork.domain.fortune.dto.request.InsertFortuneInfoRequest;
import com.gittowork.domain.fortune.dto.response.GetFortuneInfoResponse;
import com.gittowork.domain.fortune.dto.response.GetTodayFortuneResponse;
import com.gittowork.domain.fortune.service.FortuneService;
import com.gittowork.global.dto.response.ApiResponse;
import com.gittowork.global.dto.response.MessageOnlyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/fortune")
@Tag(name = "Fortune", description = "운세 정보 조회 및 저장 API")
@RequiredArgsConstructor
public class FortuneController {

    private final FortuneService fortuneService;

    @Operation(summary = "운세 정보 저장", description = "사용자의 생년월일·성별 정보를 저장하거나 업데이트합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 성공",
                    content = @Content(schema = @Schema(implementation = MessageOnlyResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 정보 없음", content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<MessageOnlyResponse> insertFortuneInfo(
            @Parameter(description = "운세 정보 저장 요청 DTO", required = true)
            @Valid @RequestBody InsertFortuneInfoRequest request) {
        return ApiResponse.success(fortuneService.insertFortuneInfo(request));
    }

    @Operation(summary = "운세 정보 조회", description = "저장된 사용자의 운세 정보를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetFortuneInfoResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "운세 정보 없음", content = @Content)
    })
    @GetMapping
    public ApiResponse<GetFortuneInfoResponse> getFortuneInfo() {
        return ApiResponse.success(fortuneService.getFortuneInfo());
    }

    @Operation(summary = "오늘의 운세 조회", description = "요청된 생년월일·성별 기반으로 오늘의 운세를 생성합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공",
                    content = @Content(schema = @Schema(implementation = GetTodayFortuneResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content)
    })
    @PostMapping(path = "/today", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<GetTodayFortuneResponse> getTodayFortune(
            @Parameter(description = "오늘의 운세 요청 DTO", required = true)
            @Valid @RequestBody GetTodayFortuneRequest request) {
        return ApiResponse.success(fortuneService.getTodayFortune(request));
    }
}
