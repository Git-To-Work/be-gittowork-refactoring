package com.gittowork.domain.interaction.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
@Schema(name = "CompanyInteractionResponse", description = "회사 상호작용 결과와 페이징 정보를 포함한 응답")
public class CompanyInteractionResponse {

    @Schema(description = "회사 상호작용 결과 리스트")
    private final List<UserInteractionResult> companies;

    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    private final Integer currentPage;

    @Schema(description = "한 페이지의 아이템 수", example = "10")
    private final Integer pageSize;

    @Schema(description = "전체 페이지 수", example = "5")
    private final Integer totalPages;

    @Schema(description = "전체 아이템 수", example = "50")
    private final Long totalItems;
}
