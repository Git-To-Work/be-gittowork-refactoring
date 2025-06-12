package com.gittowork.domain.coverletter.entity;

import com.gittowork.domain.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "cover_letter_analysis")
public class CoverLetterAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cover_letter_analysis_id", nullable = false)
    private Integer coverLetterAnalysisId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private CoverLetter file;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Lob
    @Column(name = "analysis_result", columnDefinition = "TEXT")
    private String analysisResult;

    @Column(name = "global_capability")
    private Integer globalCapability;

    @Column(name = "challenge_spirit")
    private Integer challengeSpirit;

    @Column(name = "sincerity")
    private Integer sincerity;

    @Column(name = "communication_skill")
    private Integer communicationSkill;

    @Column(name = "achievement_orientation")
    private Integer achievementOrientation;

    @Column(name = "responsibility")
    private Integer responsibility;

    @Column(name = "honesty")
    private Integer honesty;

    @Column(name = "creativity")
    private Integer creativity;

    @Column(name = "create_dttm")
    private LocalDateTime createDttm;

}
