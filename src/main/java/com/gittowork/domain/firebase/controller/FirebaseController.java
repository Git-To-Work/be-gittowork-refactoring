package com.gittowork.domain.firebase.controller;

import com.gittowork.domain.firebase.dto.request.TokenRequest;
import com.gittowork.domain.firebase.service.FirebaseService;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/firebase", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Firebase", description = "FCM 토큰 관리 API")
@RequiredArgsConstructor
public class FirebaseController {

    private final FirebaseService firebaseService;

    @Operation(summary = "FCM 토큰 등록", description = "사용자의 FCM 토큰을 등록하여 푸시 알림을 받을 수 있도록 설정합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 등록 성공",
                    content = @Content(schema = @Schema(implementation = MessageOnlyResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<MessageOnlyResponse> insertFcmToken(
            @Parameter(description = "등록할 FCM 토큰 및 사용자 정보", required = true)
            @Valid @RequestBody TokenRequest tokenRequest) {
        MessageOnlyResponse response = firebaseService.insertFcmToken(tokenRequest);
        return ApiResponse.success(HttpStatus.OK, response);
    }
}
