package com.gittowork.domain.coverletter.controller;

import com.gittowork.domain.coverletter.dto.response.GetCoverLetterAnalysisResponse;
import com.gittowork.domain.coverletter.dto.response.GetMyCoverLetterListResponse;
import com.gittowork.domain.coverletter.dto.response.UploadCoverLetterResponse;
import com.gittowork.domain.coverletter.sevice.CoverLetterService;
import com.gittowork.global.dto.response.ApiResponse;
import com.gittowork.global.dto.response.MessageOnlyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 자기소개서 업로드, 조회, 삭제 및 분석 결과 조회를 위한 REST API 컨트롤러
 */
@RestController
@RequestMapping(value = "/cover-letters", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "CoverLetter", description = "자기소개서 관리 API")
@RequiredArgsConstructor
public class CoverLetterController {
    private final CoverLetterService coverLetterService;

    @Operation(summary = "자기소개서 업로드", description = "PDF 파일과 제목을 지정하여 자기소개서를 업로드하고 분석을 시작합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "업로드 성공",
                    content = @Content(schema = @Schema(implementation = UploadCoverLetterResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadCoverLetterResponse> uploadCoverLetter(
            @Parameter(description = "업로드할 PDF 파일", required = true)
            @RequestPart @NotNull MultipartFile file,
            @Parameter(description = "자기소개서 제목", required = true)
            @RequestPart @NotNull String title) {
        return ApiResponse.success(coverLetterService.uploadCoverLetter(file, title));
    }

    @Operation(summary = "내 자기소개서 목록 조회", description = "현재 사용자 기준, 삭제되지 않은 자기소개서 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetMyCoverLetterListResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @GetMapping
    public ApiResponse<GetMyCoverLetterListResponse> getMyCoverLetterList() {
        return ApiResponse.success(coverLetterService.getMyCoverLetterList());
    }

    @Operation(summary = "자기소개서 삭제", description = "지정된 ID의 자기소개서를 soft-delete 처리하고 S3 파일을 삭제합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공",
                    content = @Content(schema = @Schema(implementation = MessageOnlyResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 자기소개서를 찾을 수 없음", content = @Content)
    })
    @DeleteMapping(path = "/{coverLetterId}")
    public ApiResponse<MessageOnlyResponse> deleteCoverLetter(
            @Parameter(description = "삭제할 자기소개서 ID", required = true)
            @PathVariable Integer coverLetterId) {
        return ApiResponse.success(coverLetterService.deleteCoverLetter(coverLetterId));
    }

    @Operation(summary = "자기소개서 분석 결과 조회", description = "지정된 자기소개서의 AI 분석 결과를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetCoverLetterAnalysisResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 분석 결과를 찾을 수 없음", content = @Content)
    })
    @GetMapping(path = "/{coverLetterId}/analysis")
    public ApiResponse<GetCoverLetterAnalysisResponse> getCoverLetterAnalysis(
            @Parameter(description = "조회할 자기소개서 ID", required = true)
            @PathVariable Integer coverLetterId) {
        return ApiResponse.success(coverLetterService.getCoverLetterAnalysis(coverLetterId));
    }
}
