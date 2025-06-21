package com.gittowork.domain.interaction.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class InteractionPostRequest {

    @NotNull(message = "companyId는 필수 입력값입니다.")
    @Min(value = 1, message = "companyId는 1 이상의 값이어야 합니다.")
    @Schema(description = "상호작용할 회사의 ID", example = "123")
    private Integer companyId;
}