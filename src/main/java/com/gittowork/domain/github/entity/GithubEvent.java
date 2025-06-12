package com.gittowork.domain.github.entity;

import com.gittowork.domain.github.model.event.Event;
import org.springframework.data.annotation.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "github_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GithubEvent {

    @Id
    private String githubEventId;

    private int userId;

    private Event events;
}
