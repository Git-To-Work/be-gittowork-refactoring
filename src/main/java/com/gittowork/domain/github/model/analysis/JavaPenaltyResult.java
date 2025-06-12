package com.gittowork.domain.github.model.analysis;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class JavaPenaltyResult {
    private double penalty;
    private int blockerCount;
    private int criticalCount;
    private int majorCount;
    private int minorCount;
    private int infoCount;
}
