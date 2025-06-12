package com.gittowork.domain.coverletter.controller;

import com.gittowork.domain.coverletter.dto.response.GetCoverLetterAnalysisResponse;
import com.gittowork.domain.coverletter.dto.response.GetMyCoverLetterListResponse;
import com.gittowork.domain.coverletter.dto.response.UploadCoverLetterResponse;
import com.gittowork.domain.coverletter.sevice.CoverLetterService;
import com.gittowork.global.response.ApiResponse;
import com.gittowork.global.response.MessageOnlyResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/cover-letter")
@RequiredArgsConstructor
public class CoverLetterController {
    private final CoverLetterService coverLetterService;

    @PostMapping("/create")
    public ApiResponse<UploadCoverLetterResponse> uploadCoverLetter(@RequestParam @NotNull MultipartFile file, @RequestParam @NotNull String title) {
        return ApiResponse.success(HttpStatus.OK, coverLetterService.uploadCoverLetter(file, title));
    }

    @GetMapping("/select/list")
    public ApiResponse<GetMyCoverLetterListResponse> getMyCoverLetterList() {
        return ApiResponse.success(HttpStatus.OK, coverLetterService.getMyCoverLetterList());
    }

    @DeleteMapping("/delete")
    public ApiResponse<MessageOnlyResponse> deleteCoverLetter(@RequestParam @NotNull int coverLetterId) {
        return ApiResponse.success(HttpStatus.OK, coverLetterService.deleteCoverLetter(coverLetterId));
    }

    @GetMapping("/select/analysis")
    public ApiResponse<GetCoverLetterAnalysisResponse> getCoverLetterAnalysis(@RequestParam @NotNull int coverLetterId) {
        return ApiResponse.success(HttpStatus.OK, coverLetterService.getCoverLetterAnalysis(coverLetterId));
    }
}
