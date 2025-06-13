package com.gittowork.domain.api.dto.response;

import lombok.*;

@Getter
@AllArgsConstructor
@Builder
public class ApiVersionResponse {
    private final String version;
    private final String releaseDate;
}
