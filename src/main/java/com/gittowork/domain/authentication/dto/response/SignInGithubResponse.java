package com.gittowork.domain.authentication.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * GitHub OAuth 로그인 및 온보딩 결과를 반환하는 응답 모델
 */
@Schema(name = "SignInGithubResponse", description = "GitHub 로그인 및 온보딩 토큰 정보")
@Getter
@AllArgsConstructor
@Builder
public class SignInGithubResponse {

    @Schema(description = "사용자 닉네임", example = "johndoe")
    private final String nickname;

    @Schema(description = "개인정보 처리방침 동의 여부", example = "true")
    private final Boolean privacyPolicyAgreed;

    @Schema(description = "사용자 아바타 이미지 URL", example = "https://avatars.githubusercontent.com/u/123456?v=4")
    private final String avatarUrl;

    @Schema(description = "발급된 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private final String accessToken;

    @Schema(description = "온보딩 필요 여부 (true: 온보딩 필요)", example = "false")
    private final Boolean onboarding;
}
