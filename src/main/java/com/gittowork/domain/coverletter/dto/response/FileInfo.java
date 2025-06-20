package com.gittowork.domain.coverletter.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "사용자의 자기소개서 파일 정보를 나타내는 DTO")
@Getter
@AllArgsConstructor
@Builder
public class FileInfo {

    @Schema(description = "파일 고유 ID", example = "1")
    private final Integer fileId;

    @Schema(description = "원본 파일명", example = "cover_letter.pdf")
    private final String fileName;

    @Schema(description = "자기소개서 제목", example = "지원 동기 및 경력 소개")
    private final String title;

    @Schema(description = "S3에 저장된 파일의 접근 URL", example = "https://bucket.s3.amazonaws.com/abc12345.pdf")
    private final String fileUrl;

    @Schema(description = "파일 업로드 시각 (yyyy-MM-dd HH:mm:ss)", example = "2025-06-21 10:15:30")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime createDttm;
}
