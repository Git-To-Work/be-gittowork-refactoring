package com.gittowork.domain.quiz.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
@Schema(name = "QuizResponse", description = "퀴즈 문제 및 선택지를 포함한 응답 정보")
public class QuizResponse {

    @Schema(description = "문제 고유 ID", example = "101")
    private final Integer questionId;

    @Schema(description = "퀴즈 유형 (예: CL, CS, FI, SS)", example = "CS")
    private final String type;

    @Schema(description = "퀴즈 카테고리 (예: computer-science)", example = "computer-science")
    private final String category;

    @Schema(description = "문제 내용 텍스트", example = "Java에서 null과 Optional의 차이는 무엇인가요?")
    private final String questionText;

    @Schema(description = "문제의 선택지 목록", example = "[\"A: 값이 없음을 나타냄\", \"B: 참조 오브젝트 초기화 표시\", \"C: Optional로 null 처리\", \"D: 둘 다 같다\"]")
    private final List<String> choices;

    @Schema(description = "정답 선택지 인덱스 (0부터 시작)", example = "2")
    private final Integer correctAnswerIndex;

    @Schema(description = "문제 해설 또는 피드백", example = "Optional은 null 처리를 안전하게 지원합니다.")
    private final String feedback;
}
