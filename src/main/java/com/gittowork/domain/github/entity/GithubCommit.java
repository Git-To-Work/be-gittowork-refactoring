package com.gittowork.domain.github.entity;

import com.gittowork.domain.github.model.commit.Commit;
import org.springframework.data.annotation.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "github_commit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GithubCommit {

    @Id
    private String githubCommitId;

    private int userId;

    private int repoId;

    private List<Commit> commits;
}
