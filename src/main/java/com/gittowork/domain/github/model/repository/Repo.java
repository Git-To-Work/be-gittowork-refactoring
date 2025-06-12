package com.gittowork.domain.github.model.repository;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Repo {
    private int repoId;
    private String repoName;
}
