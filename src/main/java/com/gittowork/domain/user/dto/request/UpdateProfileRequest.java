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
@Schema(
        name = "UpdateProfileRequest",
        description = "사용자 프로필 기본 정보를 수정하기 위한 요청 모델"
)
public class UpdateProfileRequest {

    @Schema(description = "표시할 이름", example = "홍길동")
    @NotNull(message = "이름은 필수입니다.")
    private String name;

    @Schema(description = "생년월일 (yyyy-MM-dd)", example = "1990-05-21")
    @NotNull(message = "생년월일은 필수입니다.")
    @Past(message = "생년월일은 과거 날짜여야 합니다.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate birthDt;

    @Schema(description = "경력(년 단위)", example = "3")
    @NotNull(message = "경력은 필수입니다.")
    private Integer experience;

    @Schema(description = "전화번호 (숫자만, 9~11자리)", example = "01012345678")
    @NotNull(message = "전화번호는 필수입니다.")
    @Pattern(regexp = "\\d{9,11}", message = "전화번호는 숫자만 9~11자리로 입력해야 합니다.")
    private String phone;

    @Schema(description = "알림 수신 동의 여부", example = "true")
    @NotNull(message = "알림 수신 동의 여부는 필수입니다.")
    private Boolean notificationAgreed;
}
