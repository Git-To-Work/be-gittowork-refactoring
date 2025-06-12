package com.gittowork.domain.github.controller;

import com.gittowork.domain.github.dto.request.CreateAnalysisByRepositoryRequest;
import com.gittowork.domain.github.dto.request.SaveSelectedRepositoriesRequest;
import com.gittowork.domain.github.dto.response.*;
import com.gittowork.domain.github.service.GithubService;
import com.gittowork.global.response.ApiResponse;
import com.gittowork.global.response.MessageOnlyResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/github")
@RequiredArgsConstructor
@Tag(name="Github Analysis")
public class GithubAnalysisController {

    private final GithubService githubService;

    @GetMapping("/select/analysis-by-repository")
    public ApiResponse<GithubAnalysisResponse> getGithubAnalysisByRepository(@NotNull @RequestParam String selectedRepositoryId) {
        return ApiResponse.success(githubService.getGithubAnalysisByRepository(selectedRepositoryId));
    }

    @PostMapping("/create/analysis-by-repository")
    public ApiResponse<CreateGithubAnalysisByRepositoryResponse> createAnalysisByRepository(@NotNull @RequestBody CreateAnalysisByRepositoryRequest createAnalysisByRepositoryRequest) {
        return ApiResponse.success(HttpStatus.OK, githubService.createGithubAnalysisByRepositoryResponse(createAnalysisByRepositoryRequest.getRepositories()));
    }

    @PostMapping("/create/save-selected-repository")
    public ApiResponse<SaveSelectedRepositoriesResponse> saveSelectedRepositories(@NotNull @RequestBody SaveSelectedRepositoriesRequest saveSelectedRepositoriesRequest) {
        return ApiResponse.success(HttpStatus.OK, githubService.saveSelectedGithubRepository(saveSelectedRepositoriesRequest.getRepositories()));
    }

    @GetMapping("/select/my-repository")
    public ApiResponse<GetMyRepositoryResponse> myRepository() {
        return ApiResponse.success(HttpStatus.OK, githubService.getMyRepository());
    }

    @GetMapping("/select/my-repository-combination")
    public ApiResponse<GetMyRepositoryCombinationResponse> myRepositoryCombination() {
        return ApiResponse.success(HttpStatus.OK, githubService.getMyRepositoryCombination());
    }

    @DeleteMapping("/delete/my-repository-combination")
    public ApiResponse<MessageOnlyResponse> deleteGithubAnalysisByRepository(@NotNull @RequestParam String selectedRepositoryId) {
        return ApiResponse.success(HttpStatus.OK, githubService.deleteSelectedGithubRepository(selectedRepositoryId));
    }

    @PutMapping("/update/github-data")
    public ApiResponse<MessageOnlyResponse> updateGithubData() {
        return ApiResponse.success(HttpStatus.OK, githubService.updateGithubData());
    }

}
