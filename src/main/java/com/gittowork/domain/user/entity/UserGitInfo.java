package com.gittowork.domain.user.entity;

import com.gittowork.global.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;


@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_git_info")
public class UserGitInfo extends BaseEntity {

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

}
