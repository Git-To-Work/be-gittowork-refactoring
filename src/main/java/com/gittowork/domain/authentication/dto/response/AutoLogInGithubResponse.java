package com.gittowork.domain.authentication.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoLogInGithubResponse {
    private String nickname;
    private boolean privacyPolicyAgreed;
    private String avatarUrl;
}
