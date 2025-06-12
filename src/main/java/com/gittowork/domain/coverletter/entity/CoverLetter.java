package com.gittowork.domain.coverletter.entity;

import com.gittowork.domain.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "cover_letter")
public class CoverLetter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Size(max = 255)
    @NotNull
    @Column(name = "origin_name", nullable = false)
    private String originName;

    @Size(max = 255)
    @NotNull
    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @NotNull
    @Column(name = "create_dttm", nullable = false)
    private LocalDateTime createDttm;

    @Size(max = 255)
    @NotNull
    @Column(name = "title", nullable = false)
    private String title;

    @OneToMany(mappedBy = "file")
    private Set<CoverLetterAnalysis> coverLetterAnalysis = new LinkedHashSet<>();

}
