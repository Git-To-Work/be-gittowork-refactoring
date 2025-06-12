package com.gittowork.domain.github.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetGithubAnalysisStatusResponse implements GithubAnalysisResponse {
    private String status;
    private String selectedRepositoryId;
    private List<String> selectedRepositories;
    private List<Integer> selectedRepositoryIds;
    private String message;
}
