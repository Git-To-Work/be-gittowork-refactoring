package com.gittowork.domain.github.repository;

import com.gittowork.domain.github.entity.GithubPullRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GithubPullRequestRepository extends MongoRepository<GithubPullRequest, String> {
    Optional<List<GithubPullRequest>> findAllByRepoId(int repoId);

    boolean existsByPrId(int prId);
}
