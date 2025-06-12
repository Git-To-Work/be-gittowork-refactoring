package com.gittowork.domain.interaction.repository;

import com.gittowork.domain.interaction.entity.UserLikes;
import com.gittowork.domain.interaction.entity.UserLikesId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface UserLikesRepository extends JpaRepository<UserLikes, UserLikesId> {

    Page<UserLikes> findByUserId(@Param("userId")Integer userId, Pageable pageable);

}
