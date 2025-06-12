package com.gittowork.domain.github.model.issue;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueLabel {
    private int id;
    private String name;
    private String color;
    private String description;
}
