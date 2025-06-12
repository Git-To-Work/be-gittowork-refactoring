package com.gittowork.domain.fortune.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetTodayFortuneRequest {
    private String birthDt;
    private String sex;
    private String birthTm;
}
