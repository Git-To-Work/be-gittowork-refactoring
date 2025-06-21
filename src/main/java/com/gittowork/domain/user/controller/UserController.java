package com.gittowork.domain.user.controller;

import com.gittowork.domain.user.dto.request.InsertProfileRequest;
import com.gittowork.domain.user.dto.request.UpdateInterestsFieldsRequest;
import com.gittowork.domain.user.dto.request.UpdateProfileRequest;
import com.gittowork.domain.user.dto.response.GetMyInterestFieldResponse;
import com.gittowork.domain.user.dto.response.GetMyProfileResponse;
import com.gittowork.domain.user.service.UserService;
import com.gittowork.global.dto.response.ApiResponse;
import com.gittowork.global.dto.response.MessageOnlyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "유저 관련 CRUD")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Insert Profile", description = "새로운 사용자 프로필을 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 입력"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PreAuthorize("hasAuthority('ROLE_ONBOARDING')")
    @PostMapping("/profile")
    public ApiResponse<MessageOnlyResponse> insertProfile(
            @Parameter(description = "프로필 생성 정보", required = true)
            @Valid @RequestBody InsertProfileRequest insertProfileRequest
    ) {
        return ApiResponse.success(HttpStatus.OK,
                userService.insertProfile(insertProfileRequest)
        );
    }

    @Operation(summary = "Get My Profile", description = "인증된 사용자의 프로필을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로필 정보 없음")
    })
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @GetMapping("/profile")
    public ApiResponse<GetMyProfileResponse> getMyProfile() {
        return ApiResponse.success(HttpStatus.OK,
                userService.getMyProfile()
        );
    }

    @Operation(summary = "Update Profile", description = "인증된 사용자 프로필을 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 입력"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로필 정보 없음")
    })
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @PutMapping("/profile")
    public ApiResponse<MessageOnlyResponse> updateProfile(
            @Parameter(description = "수정할 프로필 정보", required = true)
            @Valid @RequestBody UpdateProfileRequest updateProfileRequest
    ) {
        return ApiResponse.success(HttpStatus.OK,
                userService.updateProfile(updateProfileRequest)
        );
    }

    @Operation(summary = "Update Interest Field", description = "사용자의 관심 분야를 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "관심 분야 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 입력"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @PutMapping("/interest-field")
    public ApiResponse<MessageOnlyResponse> updateInterestField(
            @Parameter(description = "수정할 관심 분야 ID 리스트", required = true)
            @Valid @RequestBody UpdateInterestsFieldsRequest updateInterestsFieldsRequest
    ) {
        return ApiResponse.success(HttpStatus.OK,
                userService.updateInterestFields(updateInterestsFieldsRequest)
        );
    }

    @Operation(summary = "Get My Interest Field", description = "인증된 사용자의 관심 분야를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "관심 분야 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @GetMapping("/interest-field")
    public ApiResponse<GetMyInterestFieldResponse> getInterestField() {
        return ApiResponse.success(HttpStatus.OK,
                userService.getInterestFields()
        );
    }

    @Operation(summary = "Delete Account", description = "인증된 사용자의 계정을 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @DeleteMapping
    public ApiResponse<MessageOnlyResponse> deleteAccount(
            @Parameter(
                    description = "Bearer {accessToken} 형식의 인증 토큰",
                    example     = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
            )
            @RequestHeader(HttpHeaders.AUTHORIZATION)
            @NotNull
            String authorization
    ) {
        String token = authorization.substring(7);
        SecurityContextHolder.clearContext();
        return ApiResponse.success(HttpStatus.OK,
                userService.deleteAccount(token)
        );
    }

}
