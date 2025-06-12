package com.gittowork.domain.github.dto.response;

import com.gittowork.domain.github.model.repository.Repo;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetMyRepositoryResponse {
    private List<Repo> repositories;
}
