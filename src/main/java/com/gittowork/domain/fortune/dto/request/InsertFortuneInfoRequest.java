package com.gittowork.domain.fortune.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsertFortuneInfoRequest {

    @NotNull
    private String birthDt;

    @NotNull
    private String sex;

    @NotNull
    private String birthTm;
}
