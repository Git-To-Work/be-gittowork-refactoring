package com.gittowork.domain.github.model.analysis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AIAnalysis {
    @JsonProperty("analysis_summary")
    private List<String> analysisSummary;

    @JsonProperty("improvement_suggestions")
    private List<String> improvementSuggestions;
}
