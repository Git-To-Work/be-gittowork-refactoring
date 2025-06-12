package com.gittowork.domain.authentication.controller;

import com.gittowork.domain.authentication.dto.request.SignInGithubRequest;
import com.gittowork.domain.authentication.dto.response.AutoLogInGithubResponse;
import com.gittowork.domain.authentication.dto.response.LogOutGithubResponse;
import com.gittowork.domain.authentication.dto.response.SignInGithubResponse;
import com.gittowork.domain.authentication.service.GithubAuthenticationService;
import com.gittowork.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
@AllArgsConstructor(onConstructor = @__(@Autowired))
@Tag(name = "Github Authentication", description = "Github 인증 관리")
public class GithubAuthenticationController {
    private final GithubAuthenticationService githubAuthenticationService;

    @Operation(summary = "Github OAuth", description = "깃허브 추가")
    @PostMapping("/create/signin")
    public ApiResponse<SignInGithubResponse> signInGithub(@NotNull @RequestBody SignInGithubRequest signInGithubRequest) {
        log.info("signInGithubRequest: {}", signInGithubRequest.getCode());
        return ApiResponse.success(HttpStatus.OK, githubAuthenticationService.signInGithub(signInGithubRequest.getCode()));
    }

    @Operation(summary = "Github Auto Login", description = "깃허브 자동 로그인")
    @PostMapping("/create/login")
    public ApiResponse<AutoLogInGithubResponse> autoLogInGithub() {
        return ApiResponse.success(HttpStatus.OK, githubAuthenticationService.autoLogInGithub());
    }

    @Operation(summary = "Github logout", description = "깃허브 로그아웃")
    @PostMapping("/create/logout")
    public ApiResponse<LogOutGithubResponse> logOutGithub() {
        return ApiResponse.success(HttpStatus.OK, githubAuthenticationService.logout());
    }

}
