package com.gittowork.domain.github.repository;

import com.gittowork.domain.github.entity.GithubCommit;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GithubCommitRepository extends MongoRepository<GithubCommit, Integer> {
    Optional<GithubCommit> findByRepoId(int repoId);

    Optional<GithubCommit> findByUserIdAndRepoId(int userId, int repoId);
}
