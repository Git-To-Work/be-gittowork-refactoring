package com.gittowork.domain.github.model.pullrequest;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PullRequestBranch {

    private String label;

    private String ref;

    private String sha;

    private PullRequestUser user;
}