package com.gittowork.domain.coverletter.repository;

import com.gittowork.domain.coverletter.entity.CoverLetterAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CoverLetterAnalysisRepository extends JpaRepository<CoverLetterAnalysis, Integer> {
    Optional<CoverLetterAnalysis> findByFile_Id(int fileId);
}
