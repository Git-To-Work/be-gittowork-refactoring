package com.gittowork.domain.github.model.sonar;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Measure {
    private String metric;
    private String value;
}
