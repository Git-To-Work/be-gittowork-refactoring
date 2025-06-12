package com.gittowork.domain.authentication.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignInGithubRequest {
    private String code;
}
