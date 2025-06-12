package com.gittowork.domain.github.model.analysis;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Stats {
    private int stargazersCount;
    private int commitCount;
    private int prCount;
    private int issueCount;
}
