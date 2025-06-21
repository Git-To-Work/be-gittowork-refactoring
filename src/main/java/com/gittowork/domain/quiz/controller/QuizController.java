package com.gittowork.domain.quiz.controller;

import com.gittowork.domain.quiz.dto.response.QuizResponse;
import com.gittowork.domain.quiz.service.QuizService;
import com.gittowork.global.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/quiz")
@Tag(name = "퀴즈 API", description = "개발자 퀴즈 관련 API")
public class QuizController {

    private final QuizService quizService;

    @GetMapping
    @Operation(
            summary = "퀴즈 랜덤 반환",
            description = "지정한 카테고리(category)의 퀴즈 중 하나를 랜덤으로 반환합니다.\n" +
                    "카테고리를 생략하면 전체 퀴즈 중 무작위로 선택합니다.",
            parameters = {
                    @Parameter(
                            name = "category",
                            description = "퀴즈 형식 (cover-letter:CL, computer-science:CS, fit-interview:FI, skill-stack:SS)",
                            example = "CS",
                            in = ParameterIn.QUERY,
                            required = false,
                            schema = @Schema(type = "string")
                    )
            },
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "퀴즈 조회 성공",
                            content = @Content(schema = @Schema(implementation = QuizResponse.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "잘못된 카테고리로 퀴즈를 찾을 수 없음",
                            content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
                    )
            }
    )
    public ApiResponse<QuizResponse> getDeveloperQuiz(
            @RequestParam(value = "category", required = false)
            String category
    ) {
        return ApiResponse.success(quizService.getDeveloperQuiz(category));
    }

}
