package com.gittowork.domain.user.repository;

import com.gittowork.domain.user.entity.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByGithubName(String nickname);

    @Query("SELECT u FROM User u JOIN FETCH u.userGitInfo WHERE u.githubName = :githubName")
    Optional<User> findByGithubNameWithGitInfo(@Param("githubName") String githubName);

}
