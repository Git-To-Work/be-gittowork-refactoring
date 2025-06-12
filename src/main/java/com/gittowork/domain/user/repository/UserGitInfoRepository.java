package com.gittowork.domain.user.repository;

import com.gittowork.domain.user.entity.UserGitInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGitInfoRepository extends JpaRepository<UserGitInfo, Integer> {
}
