package com.gittowork.domain.github.model.analysis;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepositoryCombination {
    private String selectedRepositoryId;
    private List<String> repositoryNames;
    private List<Integer> repositoryIds;
}
