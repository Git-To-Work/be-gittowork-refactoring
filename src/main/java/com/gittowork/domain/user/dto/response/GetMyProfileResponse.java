package com.gittowork.domain.user.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetMyProfileResponse {
    private int userId;
    private String email;
    private String name;
    private String nickname;
    private String phone;
    private String birthDt;
    private int experience;
    private String avatarUrl;
    private boolean notificationAgreed;
}
