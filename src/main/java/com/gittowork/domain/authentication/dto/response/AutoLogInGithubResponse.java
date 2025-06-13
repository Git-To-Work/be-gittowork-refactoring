package com.gittowork.domain.authentication.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 자동 로그인 요청 시 사용자의 기본 정보를 반환하는 응답 모델
 */
@Schema(name = "AutoLogInGithubResponse", description = "자동 로그인 시 반환될 사용자 정보")
@Getter
@AllArgsConstructor
@Builder
public class AutoLogInGithubResponse {

    @Schema(description = "사용자 닉네임", example = "johndoe")
    private final String nickname;

    @Schema(description = "개인정보 처리방침 동의 여부", example = "true")
    private final Boolean privacyPolicyAgreed;

    @Schema(description = "사용자 아바타 이미지 URL", example = "https://avatars.githubusercontent.com/u/123456?v=4")
    private final String avatarUrl;
}
