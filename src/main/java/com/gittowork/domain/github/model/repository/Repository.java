package com.gittowork.domain.github.model.repository;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Repository {

    private int repoId;

    private String repoName;

    private String fullName;

    private String language;

    private int stargazersCount;

    private int forksCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime pushedAt;

    private String description;
}
