package com.gittowork.domain.fortune.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "운세 정보 저장 요청 DTO")
@Getter
@Setter
@NoArgsConstructor
public class InsertFortuneInfoRequest {

    @Schema(description = "생년월일 (yyyy-MM-dd 형식)", example = "1990-05-20")
    @NotNull
    private String birthDt;

    @Schema(description = "성별 (예: 'M' 또는 'F')", example = "F")
    @NotNull
    private String sex;

    @Schema(description = "출생 시간 (HH:mm 형식)", example = "08:45")
    @NotNull
    private String birthTm;
}
