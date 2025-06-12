package com.gittowork.domain.github.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveSelectedRepositoriesRequest {
    private int[] repositories;
}
