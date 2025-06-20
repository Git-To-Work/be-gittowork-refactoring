package com.gittowork.domain.fortune.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "오늘의 운세 조회 요청 DTO")
@Getter
@Setter
@NoArgsConstructor
public class GetTodayFortuneRequest {

    @Schema(description = "생년월일 (yyyy-MM-dd 형식)", example = "1990-05-20")
    private String birthDt;

    @Schema(description = "성별 (예: 'M' 또는 'F')", example = "M")
    private String sex;

    @Schema(description = "출생 시간 (HH:mm 형식)", example = "14:30")
    private String birthTm;
}
