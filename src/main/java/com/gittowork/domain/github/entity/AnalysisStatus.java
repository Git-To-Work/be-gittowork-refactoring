package com.gittowork.domain.github.entity;

import com.gittowork.domain.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "analysis_status")
public class AnalysisStatus {

    public enum Status {
        PENDING, ANALYZING, COMPLETE, FAIL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_status_id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Size(max = 255)
    @NotNull
    @Column(name = "selected_repositories_id", nullable = false)
    private String selectedRepositoriesId;

    @ColumnDefault("'pending'")
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

}
