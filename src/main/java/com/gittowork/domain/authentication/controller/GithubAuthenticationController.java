package com.gittowork.domain.authentication.controller;

import com.gittowork.domain.authentication.dto.response.AutoLogInGithubResponse;
import com.gittowork.domain.authentication.service.GithubAuthenticationService;
import com.gittowork.global.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/auth")
@AllArgsConstructor(onConstructor = @__(@Autowired))
@Tag(name = "Github Authentication", description = "GitHub OAuth 인증 관리 API")
public class GithubAuthenticationController {

    private final GithubAuthenticationService githubAuthenticationService;

    @Operation(
            summary = "GitHub OAuth 로그인 시작",
            description = "클라이언트 앱에서 호출 시 GitHub OAuth2 인가 코드 요청 페이지로 리다이렉트합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "302", description = "GitHub 인가 페이지로 리다이렉트"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PermitAll
    @GetMapping("/signin")
    public void redirectToGithub(
            HttpServletResponse response
    ) throws IOException {
        response.sendRedirect("/oauth2/authorization/github");
    }

    @Operation(
            summary = "GitHub 자동 로그인",
            description = "헤더의 accessToken을 검증하여 사용자 정보를 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "자동 로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "헤더 토큰 없음 또는 인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "온보딩 미완료 사용자 접근 금지"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 정보 없음")
    })
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @PostMapping("/login")
    public ApiResponse<AutoLogInGithubResponse> autoLogInGithub() {
        AutoLogInGithubResponse data = githubAuthenticationService.autoLogInGithub();
        return ApiResponse.success(HttpStatus.OK, data);
    }

    @Operation(
            summary = "GitHub 로그아웃",
            description = "헤더의 accessToken을 블랙리스트에 등록하고 refreshToken을 삭제합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (토큰 누락 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @PostMapping("/logout")
    public ApiResponse<Void> logOutGithub(
            @Parameter(
                    description = "Bearer {accessToken} 형식의 인증 토큰",
                    example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
            )
            @RequestHeader(HttpHeaders.AUTHORIZATION)
            @NotNull
            String authorization
    ) {
        String token = authorization.substring(7);
        githubAuthenticationService.logout(token);
        return ApiResponse.success();
    }

}
