package com.gittowork.domain.github.entity;

import com.gittowork.domain.github.model.repository.Repository;
import org.springframework.data.annotation.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "github_repository")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GithubRepository {

    @Id
    private String githubRepositoryId;

    private int userId;

    private List<Repository> repositories;
}
