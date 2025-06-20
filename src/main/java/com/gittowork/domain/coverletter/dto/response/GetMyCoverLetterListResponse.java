package com.gittowork.domain.coverletter.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Schema(description = "현재 사용자의 자기소개서 목록 응답 DTO")
@Getter
@AllArgsConstructor
@Builder
public class GetMyCoverLetterListResponse {

    @Schema(description = "삭제되지 않은 자기소개서 파일 정보 목록")
    private final List<FileInfo> files;
}
