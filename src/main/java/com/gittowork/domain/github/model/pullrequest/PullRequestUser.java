package com.gittowork.domain.github.model.pullrequest;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PullRequestUser {
    private String login;
    private int id;
}
