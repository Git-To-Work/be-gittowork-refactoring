package com.gittowork.domain.quiz.service;

import com.gittowork.domain.quiz.dto.response.QuizResponse;
import com.gittowork.domain.quiz.entity.Quiz;
import com.gittowork.domain.quiz.repository.QuizRepository;
import com.gittowork.global.exception.quiz.WrongQuizTypeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 퀴즈(문제) 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 개발자용 퀴즈를 랜덤으로 조회하여 {@link QuizResponse} 형태로 반환합니다.
 */
@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;

    /**
     * 지정된 카테고리의 퀴즈를 랜덤으로 한 문제 조회합니다.
     * 카테고리가 null 또는 빈 문자열일 경우 전체 퀴즈 목록에서 무작위로 선택합니다.
     *
     * @param category 퀴즈 카테고리 (예: "Java", "Spring");
     *                 null 또는 빈 문자열이면 전체 퀴즈 대상
     * @return 선택된 퀴즈 정보를 담은 {@link QuizResponse}
     * @throws WrongQuizTypeException 조회된 퀴즈가 없을 경우 발생 (잘못된 카테고리)
     */
    public QuizResponse getDeveloperQuiz(String category) {
        List<Quiz> quizzes;
        if (!StringUtils.hasText(category)) {
            quizzes = quizRepository.findAll();
        } else {
            quizzes = quizRepository.findByCategory(category);
        }

        if (quizzes.isEmpty()) {
            throw new WrongQuizTypeException("Wrong quiz category: " + category);
        }

        // 랜덤으로 하나 선택
        Quiz selected = quizzes.get(ThreadLocalRandom.current().nextInt(quizzes.size()));

        return QuizResponse.builder()
                .questionId(selected.getQuestionId())
                .type(selected.getType())
                .category(selected.getCategory())
                .questionText(selected.getQuestionText())
                .choices(selected.getChoices())
                .correctAnswerIndex(selected.getCorrectAnswerIndex())
                .feedback(selected.getFeedback())
                .build();
    }
}
