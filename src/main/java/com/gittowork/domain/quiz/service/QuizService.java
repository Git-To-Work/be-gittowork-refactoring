package com.gittowork.domain.quiz.service;

import com.gittowork.domain.quiz.dto.response.QuizResponse;
import com.gittowork.domain.quiz.entity.Quiz;
import com.gittowork.domain.quiz.repository.QuizRepository;
import com.gittowork.global.exception.WrongQuizTypeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;

    /**
     * 1. 메서드 설명 :
     *      지정한 카테고리(category)의 퀴즈 중 하나를 무작위로 반환합니다.
     * 2. 로직:
     *      - category가 비어 있으면 전체 퀴즈를 조회하고,
     *      - 값이 있으면 해당 category의 퀴즈만 조회합니다.
     *      - 퀴즈 리스트가 비어 있다면 WrongQuizTypeException 예외를 발생시킵니다.
     *      - 퀴즈 리스트 중 하나를 무작위로 선택해 QuizResponse 형태로 반환합니다.
     * 3. param:
     *      -category 퀴즈 카테고리 (예: "CS", "CL", "FI", "SS"). null 또는 빈 문자열이면 전체 퀴즈 대상.
     * 4. return:
     *      -무작위로 선택된 퀴즈에 대한 QuizResponse 객체
     * 5. 예외:
     *      - WrongQuizTypeException 해당 카테고리의 퀴즈가 존재하지 않을 경우 발생
     */

    public QuizResponse getDeveloperQuiz(String category){
        List<Quiz> quizzes;
        if(!StringUtils.hasText(category)){
            quizzes = quizRepository.findAll();
        }else {
            quizzes = quizRepository.findByCategory(category);
        }

        if(quizzes.isEmpty()){
            throw new WrongQuizTypeException("Wrong quiz category" + category);
        }

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
