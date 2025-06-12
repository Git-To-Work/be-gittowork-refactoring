package com.gittowork.domain.github.entity;

import com.gittowork.domain.github.model.issue.IssueLabel;
import com.gittowork.domain.github.model.issue.IssueUser;
import org.springframework.data.annotation.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "github_issue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GithubIssue {
    @Id
    private String githubIssueId;

    private int repoId;

    private long issueId;

    private String url;

    private String commentsUrl;

    private String title;

    private String body;

    private IssueUser user;

    private List<IssueLabel> labels;

    private IssueUser assignee;

    private List<IssueUser> assignees;

    private int comments;
}

