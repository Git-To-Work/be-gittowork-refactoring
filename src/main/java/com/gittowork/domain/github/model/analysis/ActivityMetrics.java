package com.gittowork.domain.github.model.analysis;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ActivityMetrics {
    private int totalStars;
    private int totalCommits;
    private int totalPRs;
    private int totalIssues;
}
