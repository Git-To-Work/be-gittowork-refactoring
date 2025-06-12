package com.gittowork.domain.github.dto.response;

import java.util.List;

public interface GithubAnalysisResponse {
    String getStatus();
    String getSelectedRepositoryId();
    List<String> getSelectedRepositories();
    List<Integer> getSelectedRepositoryIds();
}
