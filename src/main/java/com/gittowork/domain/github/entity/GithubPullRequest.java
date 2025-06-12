package com.gittowork.domain.github.entity;

import com.gittowork.domain.github.model.pullrequest.PullRequestBranch;
import com.gittowork.domain.github.model.pullrequest.PullRequestUser;
import org.springframework.data.annotation.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "github_pull_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GithubPullRequest {

    @Id
    private String githubPullRequestId;

    private int repoId;

    private int prId;

    private String url;

    private String htmlUrl;

    private String diffUrl;

    private String patchUrl;

    private String title;

    private String body;

    private int commentsCount;

    private int reviewCommentsCount;

    private int commitsCount;

    private PullRequestUser user;

    private PullRequestBranch head;

    private PullRequestBranch base;

}
