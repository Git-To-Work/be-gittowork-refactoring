package com.gittowork.domain.coverletter.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoverLetterAnalysisStat {
    private int globalCapability;
    private int challengeSpirit;
    private int sincerity;
    private int communicationSkill;
    private int achievementOrientation;
    private int responsibility;
    private int honesty;
    private int creativity;
}
