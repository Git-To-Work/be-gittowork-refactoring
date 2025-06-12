package com.gittowork.domain.github.model.issue;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueUser {
    private String login;
    private int id;
}
