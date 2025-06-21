package com.gittowork.domain.techstack.entity;

import com.gittowork.domain.jobnotice.entity.JobNotice;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "notice_tech_stack",
        indexes = {
                @Index(name = "idx_notice_tech_stack_job_notice_id", columnList = "job_notice_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeTechStack {

    @EmbeddedId
    private NoticeTechStackId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_notice_id", insertable = false, updatable = false)
    private JobNotice jobNotice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tech_stack_id", insertable = false, updatable = false)
    private TechStack techStack;
}
