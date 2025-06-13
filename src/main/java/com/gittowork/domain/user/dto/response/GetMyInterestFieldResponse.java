package com.gittowork.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

/**
 * 인증된 사용자의 관심 분야 ID 및 이름 목록을 반환하는 응답 모델
 */
@Schema(
        name = "GetMyInterestFieldResponse",
        description = "사용자의 관심 분야 ID 및 이름 목록을 포함한 응답 모델"
)
@Getter
@AllArgsConstructor
@Builder
public class GetMyInterestFieldResponse {

    @Schema(
            description = "관심 분야 이름 리스트",
            example = "[\"백엔드\", \"프론트엔드\", \"데브옵스\"]"
    )
    private final List<String> interestsFields;

    @Schema(
            description = "관심 분야 ID 리스트",
            example = "[1, 2, 3]"
    )
    private final List<Integer> interestsFieldIds;
}
