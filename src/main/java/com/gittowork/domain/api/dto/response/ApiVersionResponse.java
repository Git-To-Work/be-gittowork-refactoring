package com.gittowork.domain.api.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiVersionResponse {
    private String version;
    private String releaseDate;
}
