package com.gittowork.domain.github.repository;

import com.gittowork.domain.github.entity.AnalysisStatus;
import com.gittowork.domain.user.entity.User;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnalysisStatusRepository extends JpaRepository<AnalysisStatus, Integer> {
    Optional<AnalysisStatus> findByUserAndSelectedRepositoriesId(@NotNull User user, @NotNull String selectedRepositoriesId);

    Optional<AnalysisStatus> findBySelectedRepositoriesId(String selectedRepositoryId);

    @Modifying
    @Query("UPDATE AnalysisStatus s " +
            " SET s.status = :newStatus " +
            " WHERE s.user.id = :userId " +
            "   AND s.selectedRepositoriesId = :selId")
    void updateStatus(@Param("userId") int userId,
                     @Param("selId")  String selectedRepositoriesId,
                     @Param("newStatus") AnalysisStatus.Status newStatus);

    @Query("update AnalysisStatus a set a.status = :status where a.user.id = :userId and a.selectedRepositoriesId = :selectedRepositoriesId")
    @Modifying
    void updateStatusByUserIdAndSelectedRepositoriesId(Integer userId, @Size(max = 255) @NotNull String selectedRepositoriesId, AnalysisStatus.Status status);
}
