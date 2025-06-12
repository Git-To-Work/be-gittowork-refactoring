package com.gittowork.domain.github.entity;

import com.gittowork.domain.github.model.analysis.AIAnalysis;
import com.gittowork.domain.github.model.analysis.ActivityMetrics;
import com.gittowork.domain.github.model.analysis.RepositoryResult;
import com.gittowork.domain.github.model.repository.Repository;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "github_analysis_result")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class GithubAnalysisResult {

    @Id
    private String githubAnalysisResultId;

    private long userId;
    private LocalDateTime analysisDate;
    private String selectedRepositoriesId;
    private List<Repository> selectedRepositories;
    private Map<String, Double> languageRatios;

    private List<RepositoryResult> repositories;

    private int overallScore;
    private String primaryRole;
    private int roleScores;
    private ActivityMetrics activityMetrics;

    private AIAnalysis aiAnalysis;
}
