package com.gittowork.domain.interaction.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteractionGetRequest {
    private int page = 0;
    private int size = 20;
}
