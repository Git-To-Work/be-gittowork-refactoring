package com.gittowork.domain.firebase.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetTokenRequest {
    @NotNull
    private String fcmToken;
}
