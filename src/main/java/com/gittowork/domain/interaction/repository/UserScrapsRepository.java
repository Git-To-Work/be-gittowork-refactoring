package com.gittowork.domain.interaction.repository;

import com.gittowork.domain.interaction.entity.UserScraps;
import com.gittowork.domain.interaction.entity.UserScrapsId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface UserScrapsRepository extends JpaRepository<UserScraps, UserScrapsId> {

    Page<UserScraps> findByUserId(@Param("userId") int userId, Pageable pageable);

}
