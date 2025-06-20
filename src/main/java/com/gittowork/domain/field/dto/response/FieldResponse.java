package com.gittowork.domain.field.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "관심 분야 정보를 나타내는 DTO")
@Getter
@AllArgsConstructor
@Builder
public class FieldResponse {

    @Schema(description = "분야명", example = "백엔드 개발")
    private final String fieldName;

    @Schema(description = "분야 로고 이미지 URL", example = "https://cdn.example.com/logos/backend.png")
    private final String fieldLogoUrl;
}
