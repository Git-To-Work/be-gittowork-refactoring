package com.gittowork.domain.jobnotice.entity;

import com.gittowork.domain.company.entity.Company;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_notice")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobNotice {

    @Id
    @Column(name = "job_notice_id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Size(max = 255)
    @NotNull
    @Column(name = "job_notice_title", nullable = false)
    private String jobNoticeTitle;

    @NotNull
    @Column(name = "deadline_dttm", nullable = false)
    private LocalDateTime deadlineDttm;

    @Size(max = 255)
    @Column(name = "location")
    private String location;

    @ColumnDefault("1")
    @Column(name = "newcomer")
    private Boolean newcomer;

    @Column(name = "min_career")
    private Integer minCareer;

    @Column(name = "max_career")
    private Integer maxCareer;

}
