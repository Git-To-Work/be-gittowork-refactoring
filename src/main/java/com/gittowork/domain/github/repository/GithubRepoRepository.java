package com.gittowork.domain.github.repository;

import com.gittowork.domain.github.entity.GithubRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GithubRepoRepository extends MongoRepository<GithubRepository, Long> {

    Optional<GithubRepository> findByUserId(int userId);
}
