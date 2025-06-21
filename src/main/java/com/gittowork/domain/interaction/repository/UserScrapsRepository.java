package com.gittowork.domain.interaction.repository;

import com.gittowork.domain.interaction.entity.UserScraps;
import com.gittowork.domain.interaction.entity.UserScrapsId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserScrapsRepository extends JpaRepository<UserScraps, UserScrapsId> {

    @EntityGraph(attributePaths = {"company", "company.field"})
    Page<UserScraps> findByUserId(@Param("userId") Integer userId, Pageable pageable);

    @Query("SELECT s.id.companyId FROM UserScraps s WHERE s.id.userId = :userId AND s.id.companyId IN :companyIds")
    List<Integer> findCompanyIdsByUserId(@Param("userId") int userId,
                                         @Param("companyIds") List<Integer> companyIds);
}
