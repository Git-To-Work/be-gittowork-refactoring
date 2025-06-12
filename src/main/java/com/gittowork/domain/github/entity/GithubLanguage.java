package com.gittowork.domain.github.entity;

import org.springframework.data.annotation.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Document(collection = "github_language")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GithubLanguage {

    @Id
    private String githubLanguageId;

    private int userId;

    private int repoId;

    private Map<String, Long> languages;
}
