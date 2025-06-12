package com.gittowork.domain.interaction.repository;

import com.gittowork.domain.interaction.entity.UserBlacklist;
import com.gittowork.domain.interaction.entity.UserBlacklistId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface UserBlacklistRepository extends JpaRepository<UserBlacklist, UserBlacklistId> {

    Page<UserBlacklist> findByUserId(@Param("userId")Integer userId, Pageable pageable);

}
