package com.gittowork.domain.techstack.repository;

import com.gittowork.domain.techstack.entity.NoticeTechStack;
import com.gittowork.domain.techstack.entity.NoticeTechStackId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NoticeTechStackRepository extends JpaRepository<NoticeTechStack, NoticeTechStackId> {
    List<NoticeTechStack> findByJobNoticeId(Integer jobNoticeId);
}
