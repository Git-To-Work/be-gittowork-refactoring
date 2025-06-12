package com.gittowork.domain.github.repository;

import com.gittowork.domain.github.entity.GithubIssue;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GithubIssueRepository extends MongoRepository<GithubIssue, String> {
    Optional<List<GithubIssue>> findAllByRepoId(int repoId);

    boolean existsByIssueId(long issueId);
}
