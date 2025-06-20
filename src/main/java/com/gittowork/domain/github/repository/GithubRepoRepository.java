package com.gittowork.domain.github.repository;

import com.gittowork.domain.github.entity.GithubRepository;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GithubRepoRepository extends MongoRepository<GithubRepository, Long> {

    Optional<GithubRepository> findByUserId(int userId);

    @Aggregation(pipeline = {
            "{ $match: { userId: ?0 } }",
            "{ $unwind: \"$repositories\" }",
            "{ $match: { \"repositories.repoId\": { $in: ?1 } } }",
            "{ $project: { _id: 0, repoName: \"$repositories.repoName\" } }"
    })
    List<String> findRepoNamesByUserIdAndRepoIds(int userId, List<Integer> repoIds);
}
