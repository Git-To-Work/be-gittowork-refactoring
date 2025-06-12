package com.gittowork.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import javax.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateInterestsFieldsRequest {

    @NotNull
    @Size(max = 5)
    private int[] interestsFields;
}
