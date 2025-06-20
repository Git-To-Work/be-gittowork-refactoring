package com.gittowork.domain.github.controller;

import com.gittowork.domain.github.dto.request.CreateAnalysisByRepositoryRequest;
import com.gittowork.domain.github.dto.request.SaveSelectedRepositoriesRequest;
import com.gittowork.domain.github.dto.response.*;
import com.gittowork.domain.github.service.GithubService;
import com.gittowork.global.response.ApiResponse;
import com.gittowork.global.response.MessageOnlyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * GitHub 연동 및 분석 관련 API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequestMapping(value = "/github", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Github Analysis", description = "GitHub 리포지토리 분석 및 저장 API")
@RequiredArgsConstructor
public class GithubAnalysisController {

    private final GithubService githubService;

    @Operation(summary = "저장된 리포지토리 분석 조회", description = "선택된 리포지토리 ID로 저장된 분석 결과를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GithubAnalysisResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "분석 결과 없음", content = @Content)
    })
    @GetMapping("/analysis")
    public ApiResponse<GithubAnalysisResponse> getGithubAnalysisByRepository(
            @Parameter(description = "조회할 리포지토리 ID", required = true, example = "12345")
            @RequestParam @NotNull String selectedRepositoryId) {
        return ApiResponse.success(githubService.getGithubAnalysisByRepository(selectedRepositoryId));
    }

    @Operation(summary = "리포지토리 분석 생성", description = "여러 리포지토리를 선택하여 분석을 생성합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공",
                    content = @Content(schema = @Schema(implementation = CreateGithubAnalysisByRepositoryResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content)
    })
    @PostMapping(path = "/analysis", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<CreateGithubAnalysisByRepositoryResponse> createAnalysisByRepository(
            @Parameter(description = "분석할 리포지토리 목록 요청 DTO", required = true)
            @Valid @RequestBody CreateAnalysisByRepositoryRequest request) {
        return ApiResponse.success(HttpStatus.OK,
                githubService.createGithubAnalysisByRepositoryResponse(request.getRepositories()));
    }

    @Operation(summary = "선택 리포지토리 저장", description = "사용자가 선택한 리포지토리 목록을 저장합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 성공",
                    content = @Content(schema = @Schema(implementation = SaveSelectedRepositoriesResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content)
    })
    @PostMapping(path = "/repository", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<SaveSelectedRepositoriesResponse> saveSelectedRepositories(
            @Parameter(description = "저장할 리포지토리 목록 요청 DTO", required = true)
            @Valid @RequestBody SaveSelectedRepositoriesRequest request) {
        return ApiResponse.success(HttpStatus.OK,
                githubService.saveSelectedGithubRepository(request.getRepositories()));
    }

    @Operation(summary = "내 리포지토리 조회", description = "로그인된 사용자의 GitHub 리포지토리 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetMyRepositoryResponse.class)))
    })
    @GetMapping
    public ApiResponse<GetMyRepositoryResponse> myRepository() {
        return ApiResponse.success(HttpStatus.OK, githubService.getMyRepository());
    }

    @Operation(summary = "GitHub 데이터 갱신", description = "외부 GitHub API에서 최신 데이터를 가져와 업데이트 합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "갱신 성공",
                    content = @Content(schema = @Schema(implementation = MessageOnlyResponse.class)))
    })
    @PutMapping
    public ApiResponse<MessageOnlyResponse> updateGithubData() {
        return ApiResponse.success(HttpStatus.OK, githubService.updateGithubData());
    }

    @Operation(summary = "조합된 리포지토리 조회", description = "사용자가 선택한 리포지토리 조합 정보를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetMyRepositoryCombinationResponse.class)))
    })
    @GetMapping("/combination")
    public ApiResponse<GetMyRepositoryCombinationResponse> myRepositoryCombination() {
        return ApiResponse.success(HttpStatus.OK, githubService.getMyRepositoryCombination());
    }

    @Operation(summary = "선택 리포지토리 삭제", description = "선택된 리포지토리를 삭제하여 분석 결과를 제거합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공",
                    content = @Content(schema = @Schema(implementation = MessageOnlyResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "삭제할 리포지토리 없음", content = @Content)
    })
    @DeleteMapping("/combination")
    public ApiResponse<MessageOnlyResponse> deleteGithubAnalysisByRepository(
            @Parameter(description = "삭제할 리포지토리 ID", required = true, example = "12345")
            @RequestParam @NotNull String selectedRepositoryId) {
        return ApiResponse.success(HttpStatus.OK,
                githubService.deleteSelectedGithubRepository(selectedRepositoryId));
    }
}
