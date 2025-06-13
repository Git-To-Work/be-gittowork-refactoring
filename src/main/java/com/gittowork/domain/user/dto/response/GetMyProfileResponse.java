package com.gittowork.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;

/**
 * 인증된 사용자의 프로필 정보를 반환하는 응답 모델
 */
@Schema(
        name = "GetMyProfileResponse",
        description = "사용자의 프로필 정보를 포함한 응답 모델"
)
@Getter
@AllArgsConstructor
@Builder
public class GetMyProfileResponse {

    @Schema(description = "사용자 ID", example = "123")
    private final Integer userId;

    @Schema(description = "GitHub 이메일 주소", example = "user@example.com")
    private final String email;

    @Schema(description = "실명 또는 표시할 이름", example = "홍길동")
    private final String name;

    @Schema(description = "GitHub 로그인 닉네임", example = "johndoe")
    private final String nickname;

    @Schema(description = "전화번호", example = "01012345678")
    private final String phone;

    @Schema(description = "생년월일 (yyyy-MM-dd)", example = "1990-05-21")
    private final LocalDate birthDt;

    @Schema(description = "경력(년 단위)", example = "3")
    private final Integer experience;

    @Schema(description = "아바타 이미지 URL", example = "https://avatars.githubusercontent.com/u/123456?v=4")
    private final String avatarUrl;

    @Schema(description = "알림 수신 동의 여부", example = "true")
    private final Boolean notificationAgreed;
}
