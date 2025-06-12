package com.gittowork.domain.github.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGithubAnalysisByRepositoryResponse {
    private boolean analysisStarted;
    private String selectedRepositoryId;
    private List<String> selectedRepositories;
    private String message;
}
