package com.gittowork.domain.github.model.analysis;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RepositoryResult {
    private int repoId;
    private int score;
    private String insights;
    private Map<String, Integer> languages;
    private Stats stats;
    private Map<String, String> projectMeasures;
    private double commitFrequency;
}
