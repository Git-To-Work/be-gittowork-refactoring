package com.gittowork.domain.github.entity;

import com.gittowork.domain.github.model.repository.Repository;
import org.springframework.data.annotation.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "selected_repository")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SelectedRepository {

    @Id
    private String selectedRepositoryId;

    private int userId;

    private List<Repository> repositories;
}
