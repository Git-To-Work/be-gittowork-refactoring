package com.gittowork.domain.coverletter.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "AI 기반 분석 결과의 통계 점수를 나타내는 DTO")
@Getter
@AllArgsConstructor
@Builder
public class CoverLetterAnalysisStat {

    @Schema(description = "종합 역량(Global Capability) 점수", example = "85")
    private final Integer globalCapability;

    @Schema(description = "도전 정신(Challenge Spirit) 점수", example = "90")
    private final Integer challengeSpirit;

    @Schema(description = "성실성(Sincerity) 점수", example = "88")
    private final Integer sincerity;

    @Schema(description = "커뮤니케이션 역량(Communication Skill) 점수", example = "92")
    private final Integer communicationSkill;

    @Schema(description = "성취 지향(Achievement Orientation) 점수", example = "80")
    private final Integer achievementOrientation;

    @Schema(description = "책임감(Responsibility) 점수", example = "87")
    private final Integer responsibility;

    @Schema(description = "정직성(Honesty) 점수", example = "91")
    private final Integer honesty;

    @Schema(description = "창의성(Creativity) 점수", example = "89")
    private final Integer creativity;
}