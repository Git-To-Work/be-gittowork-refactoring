package com.gittowork.domain.interaction.dto.response;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pagination {
    private int currentPage;
    private int pageSize;
    private int totalPages;
    private long totalItems;
}
