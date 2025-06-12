package com.gittowork.domain.github.repository;

import com.gittowork.domain.github.entity.GithubEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface GithubEventRepository extends MongoRepository<GithubEvent, String> {

    Collection<GithubEvent> findAllByUserId(int userId);

    Optional<GithubEvent> findTopByUserIdAndEvents_RepoOrderByEventsCreatedAtDesc(int userId, String eventRepository);

    Optional<GithubEvent> findTopByUserIdOrderByEventsCreatedAtDesc(int userId);
}
