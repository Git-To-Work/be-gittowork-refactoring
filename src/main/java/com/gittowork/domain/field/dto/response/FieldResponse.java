package com.gittowork.domain.field.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class FieldResponse {

    private final String fieldName;

    private final String fieldLogoUrl;
}
