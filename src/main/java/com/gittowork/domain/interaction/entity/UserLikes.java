package com.gittowork.domain.interaction.entity;

import com.gittowork.domain.company.entity.Company;
import com.gittowork.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "user_likes",
        indexes = {
                @Index(name = "idx_user_likes_user_id", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLikes {

    @EmbeddedId
    private UserLikesId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, insertable = false, updatable = false)
    private Company company;

}
