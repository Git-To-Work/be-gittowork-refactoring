package com.gittowork.domain.github.repository;

import com.gittowork.domain.github.entity.GithubLanguage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GithubLanguageRepository extends MongoRepository<GithubLanguage, Integer> {
    Optional<GithubLanguage> findByUserIdAndRepoId(int userId, int repoId);
}
