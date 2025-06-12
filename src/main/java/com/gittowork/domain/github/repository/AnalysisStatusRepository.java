package com.gittowork.domain.github.repository;

import com.gittowork.domain.github.entity.AnalysisStatus;
import com.gittowork.domain.user.entity.User;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnalysisStatusRepository extends JpaRepository<AnalysisStatus, Integer> {
    Optional<AnalysisStatus> findByUserAndSelectedRepositoriesId(@NotNull User user, @NotNull String selectedRepositoriesId);

    Optional<AnalysisStatus> findBySelectedRepositoriesId(String selectedRepositoryId);
}
