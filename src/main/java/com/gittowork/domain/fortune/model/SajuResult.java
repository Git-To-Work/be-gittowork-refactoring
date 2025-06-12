package com.gittowork.domain.fortune.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SajuResult {
    private String yearPillar;
    private String monthPillar;
    private String dayPillar;
    private String hourPillar;
    private String sex;
}
