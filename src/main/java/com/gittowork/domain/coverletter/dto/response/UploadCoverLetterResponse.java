package com.gittowork.domain.coverletter.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "자기소개서 업로드 응답 DTO")
@Getter
@AllArgsConstructor
@Builder
public class UploadCoverLetterResponse {

    @Schema(description = "처리 결과 메시지", example = "파일 업로드가 성공적으로 완료되었으며, 분석을 시작했습니다.")
    private final String message;

    @Schema(description = "생성된 자기소개서의 ID", example = "1")
    private final Integer coverLetterId;
}
