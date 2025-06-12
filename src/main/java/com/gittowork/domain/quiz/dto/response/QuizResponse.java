package com.gittowork.domain.quiz.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizResponse {

    private int questionId;

    private String type;

    private String category;

    private String questionText;

    private List<String> choices;

    private int correctAnswerIndex;

    private String feedback;


}
