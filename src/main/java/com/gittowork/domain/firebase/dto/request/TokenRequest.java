package com.gittowork.domain.firebase.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * FCM 토큰 등록을 위한 요청 DTO 클래스입니다.
 */
@Schema(description = "FCM 토큰 등록 요청 DTO")
@Getter
@Setter
@NoArgsConstructor
public class TokenRequest {

    @Schema(description = "등록할 FCM 토큰", example = "dGhpcy1pcy1hLWZjdC10b2tlbg==", required = true)
    @NotBlank
    private String fcmToken;
}
