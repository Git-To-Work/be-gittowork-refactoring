package com.gittowork.domain.github.model.sonar;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeasuresResponse {
    private List<Measure> measures;
}
