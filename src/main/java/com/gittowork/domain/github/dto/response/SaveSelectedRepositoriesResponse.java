package com.gittowork.domain.github.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveSelectedRepositoriesResponse {
    private String message;
    private String selectedRepositoryId;
}
