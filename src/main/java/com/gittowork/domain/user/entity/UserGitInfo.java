package com.gittowork.domain.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "user_git_info")
public class UserGitInfo {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private Integer id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Size(max = 255)
    @NotNull
    @Column(name = "avatar_url", nullable = false)
    private String avatarUrl;

    @NotNull
    @Column(name = "public_repositories", nullable = false)
    private Integer publicRepositories;

    @NotNull
    @Column(name = "followers", nullable = false)
    private Integer followers;

    @NotNull
    @Column(name = "followings", nullable = false)
    private Integer followings;

    @NotNull
    @Column(name = "create_dttm", nullable = false)
    private LocalDateTime createDttm;

    @Column(name = "update_dttm")
    private LocalDateTime updateDttm;

}
