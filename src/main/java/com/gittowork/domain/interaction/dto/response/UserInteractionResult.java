package com.gittowork.domain.interaction.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
@Schema(name = "UserInteractionResult", description = "회사 ID, 이름, 로고, 분야, 기술스택, 채용공고 현황, 스크랩 여부를 포함한 상호작용 결과")
public class UserInteractionResult {

    @Schema(description = "회사 고유 ID", example = "123")
    private final Integer companyId;

    @Schema(description = "회사 명칭", example = "GitToWork Inc.")
    private final String companyName;

    @Schema(description = "회사 로고 URL", example = "https://cdn.example.com/logo.png")
    private final String logo;

    @Schema(description = "회사 분야명", example = "IT Services")
    private final String fieldName;

    @Schema(description = "채용공고 기술스택 리스트", example = "[\"Java\", \"Spring Boot\", \"AWS\"]")
    private final List<String> techStacks;

    @Schema(description = "활성 채용공고 존재 여부", example = "true")
    private final Boolean hasActiveJobNotice;

    @Schema(description = "사용자 스크랩 여부", example = "false")
    private final Boolean scrapped;
}