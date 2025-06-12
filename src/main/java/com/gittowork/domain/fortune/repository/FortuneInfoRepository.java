package com.gittowork.domain.fortune.repository;

import com.gittowork.domain.fortune.entity.FortuneInfo;
import com.gittowork.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FortuneInfoRepository extends JpaRepository<FortuneInfo, Integer> {
    Optional<FortuneInfo> findByUser(User user);
}
