package com.gittowork.domain.github.repository;

import com.gittowork.domain.github.entity.GithubAnalysisResult;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface GithubAnalysisResultRepository extends MongoRepository<GithubAnalysisResult, String> {
    Optional<GithubAnalysisResult> findFirstBySelectedRepositoriesIdOrderByAnalysisDateDesc(String selectedRepositoriesId);

    Optional<GithubAnalysisResult> findBySelectedRepositoriesId(String selectedGithubRepositoryIds);
}
