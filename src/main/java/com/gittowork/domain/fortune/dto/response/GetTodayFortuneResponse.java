package com.gittowork.domain.fortune.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 오늘의 운세 생성 결과를 응답하기 위한 DTO 클래스입니다.
 */
@Schema(description = "오늘의 운세 생성 결과 응답 DTO")
@Getter
@Setter
@AllArgsConstructor
@Builder
public class GetTodayFortuneResponse {

    @Schema(description = "오늘의 운세 상세 정보 객체")
    @JsonProperty("fortune")
    private final Fortune fortune;

    /**
     * 오늘의 운세 항목별 정보
     */
    @Schema(description = "오늘의 운세 항목별 정보 DTO")
    @Getter
    @Setter
    @AllArgsConstructor
    @Builder
    public static class Fortune {

        @Schema(description = "전반적인 운세", example = "기분 좋은 하루가 될 것입니다.")
        @JsonProperty("overall")
        private final String overall;

        @Schema(description = "재물 운세", example = "투자는 신중히 하세요.")
        @JsonProperty("wealth")
        private final String wealth;

        @Schema(description = "연애 운세", example = "친구와의 대화가 깊어집니다.")
        @JsonProperty("love")
        private final String love;

        @Schema(description = "학업/업무 운세", example = "집중력이 높아집니다.")
        @JsonProperty("study")
        private final String study;

        @Schema(description = "운세 날짜 (yyyy-MM-dd 형식)", example = "2025-06-21")
        @JsonProperty("date")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate date;
    }
}
