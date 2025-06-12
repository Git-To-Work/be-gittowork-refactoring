package com.gittowork.domain.techstack.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import org.hibernate.Hibernate;

import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class NoticeTechStackId implements java.io.Serializable {

    @Column(name = "job_notice_id", nullable = false)
    private Integer jobNoticeId;

    @Column(name = "tech_stack_id", nullable = false)
    private Integer techStackId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        NoticeTechStackId that = (NoticeTechStackId) o;
        return Objects.equals(jobNoticeId, that.jobNoticeId) &&
                Objects.equals(techStackId, that.techStackId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobNoticeId, techStackId);
    }
}
