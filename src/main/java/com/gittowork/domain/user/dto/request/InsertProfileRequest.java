package com.gittowork.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsertProfileRequest {

    @NotNull
    private int experience;

    @NotNull
    private String name;

    @NotNull
    private String birthDt;

    @NotNull
    private String phone;

    @NotNull
    private boolean privacyPolicyAgreed;

    @NotNull
    private boolean notificationAgreed;
}
