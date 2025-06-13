package com.gittowork.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Schema(
        name = "UpdateInterestsFieldsRequest",
        description = "사용자가 선택한 관심 분야 ID 리스트를 업데이트하기 위한 요청 모델"
)
public class UpdateInterestsFieldsRequest {

    @Schema(
            description = "관심 분야 ID 리스트 (최대 5개까지 선택 가능)",
            example = "[1,2,3]"
    )
    @NotNull(message = "관심 분야 리스트는 필수입니다.")
    @Size(min = 1, max = 5, message = "관심 분야는 최소 1개, 최대 5개까지 선택할 수 있습니다.")
    private List<Integer> interestsFields;
}
