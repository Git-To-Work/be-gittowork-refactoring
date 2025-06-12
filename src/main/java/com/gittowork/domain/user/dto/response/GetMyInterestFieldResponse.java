package com.gittowork.domain.user.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetMyInterestFieldResponse {

    private String[] interestsFields;

    private int[] interestsFieldIds;
}
