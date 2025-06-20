package com.gittowork.domain.coverletter.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "자기소개서 AI 분석 결과 응답 DTO")
@Getter
@AllArgsConstructor
@Builder
public class GetCoverLetterAnalysisResponse {

    @Schema(description = "분석된 자기소개서의 고유 ID", example = "1")
    private final Integer coverLetterId;

    @Schema(description = "AI가 분석한 결과 텍스트", example = "해당 자기소개서는 적극성과 책임감을 잘 드러냅니다.")
    private final String aiAnalysisResult;

    @Schema(description = "분석 통계 점수 객체")
    private final CoverLetterAnalysisStat stat;

    @Schema(description = "해당 자기소개서 파일의 URL", example = "https://bucket.s3.amazonaws.com/abc12345.pdf")
    private final String fileUrl;
}
