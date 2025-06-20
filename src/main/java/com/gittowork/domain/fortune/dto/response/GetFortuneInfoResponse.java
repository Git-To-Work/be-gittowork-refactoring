package com.gittowork.domain.fortune.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 사용자의 저장된 운세 정보를 응답하기 위한 DTO 클래스입니다.
 */
@Schema(description = "저장된 운세 정보 조회 응답 DTO")
@Getter
@AllArgsConstructor
@Builder
public class GetFortuneInfoResponse {

    @Schema(description = "생년월일 (yyyy-MM-dd 형식)", example = "1990-05-20")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private final LocalDate birthDt;

    @Schema(description = "성별 (예: 'M' 또는 'F')", example = "F")
    private final String sex;

    @Schema(description = "출생 시간 (HH:mm 형식)", example = "08:45")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private final LocalTime birthTm;
}
