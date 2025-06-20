package com.gittowork.domain.github.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class SaveSelectedRepositoriesRequest {
    private int[] repositories;
}
