package com.gittowork.domain.github.repository;

import com.gittowork.domain.github.entity.SelectedRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SelectedRepoRepository extends MongoRepository<SelectedRepository, String> {
    List<SelectedRepository> findAllByUserId(int userId);

    Optional<SelectedRepository> findByUserIdAndRepositories(int userId, List<com.gittowork.domain.github.model.repository.Repository> repositories);

    Optional<SelectedRepository> findByUserIdAndSelectedRepositoryId(int userId, String selectedRepoIdStr);
}
