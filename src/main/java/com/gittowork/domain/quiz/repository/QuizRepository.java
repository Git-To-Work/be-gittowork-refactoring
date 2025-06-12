package com.gittowork.domain.quiz.repository;

import com.gittowork.domain.quiz.entity.Quiz;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface QuizRepository extends MongoRepository<Quiz, String> {
    List<Quiz> findByCategory(String category);
}
