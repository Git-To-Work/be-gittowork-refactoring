package com.gittowork.domain.interaction.dto.response;

import lombok.*;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyInteractionResponse {
    private List<UserInteractionResult> companies;
    private Pagination pagination;
}
