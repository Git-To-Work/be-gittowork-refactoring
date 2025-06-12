package com.gittowork.domain.quiz.controller;

import com.gittowork.domain.quiz.dto.response.QuizResponse;
import com.gittowork.domain.quiz.service.QuizService;
import com.gittowork.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/quiz")
@Tag(name = "퀴즈 API", description = "개발자 퀴즈 관련 API")
public class QuizController {
    private final QuizService quizService;

    @GetMapping("/select")
    @Operation(summary = "퀴즈 랜덤 반환", description = "지정한 카테고리(category)의 퀴즈 중 하나를 랜덤으로 반환합니다.")
    public ApiResponse<QuizResponse> getDeveloperQuiz(@Parameter(description = "퀴즈 형식 (cover-letter:CL, computer-science:CS, fit-interview:FI, skill-stack:SS)", example = "CS") @RequestParam(required = false) String category) {
        return ApiResponse.success(quizService.getDeveloperQuiz(category));
    }

}
