package com.gittowork.domain.github.dto.response;

import com.gittowork.domain.github.model.analysis.RepositoryCombination;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetMyRepositoryCombinationResponse {
    private List<RepositoryCombination> repositoryCombinations;
}
