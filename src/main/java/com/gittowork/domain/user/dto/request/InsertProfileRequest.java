package com.gittowork.domain.user.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Schema(name = "InsertProfileRequest", description = "회원 프로필 추가 정보를 담은 요청 모델")
public class InsertProfileRequest {

    @Schema(description = "경력(년 단위)", example = "3")
    @NotNull
    private Integer experience;

    @Schema(description = "실명 또는 표시할 이름", example = "홍길동")
    @NotNull
    private String name;

    @Schema(
            description = "생년월일 (yyyy-MM-dd)",
            example = "1990-05-21"
    )
    @NotNull
    @Past
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate birthDt;

    @Schema(
            description = "전화번호 (숫자, 하이픈 없이)",
            example = "01012345678"
    )
    @NotNull
    @Pattern(regexp = "\\d{9,11}", message = "전화번호는 숫자만 9~11자리로 입력해야 합니다.")
    private String phone;

    @Schema(description = "개인정보 처리방침 동의 여부", example = "true")
    @NotNull
    private Boolean privacyPolicyAgreed;

    @Schema(description = "알림 수신 동의 여부", example = "false")
    @NotNull
    private Boolean notificationAgreed;
}
