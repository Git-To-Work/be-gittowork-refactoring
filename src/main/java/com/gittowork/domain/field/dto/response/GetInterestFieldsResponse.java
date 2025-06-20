package com.gittowork.domain.field.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
@Schema(
        name = "GetInterestFieldsResponse",
        description = "모든 관심 분야 목록을 포함한 응답 모델"
)
public class GetInterestFieldsResponse {

    @Schema(description = "관심 분야 리스트")
    private final List<FieldResponse> fields;
}
