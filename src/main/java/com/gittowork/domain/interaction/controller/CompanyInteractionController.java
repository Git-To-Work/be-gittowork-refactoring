package com.gittowork.domain.interaction.controller;

import com.gittowork.domain.interaction.dto.request.InteractionPostRequest;
import com.gittowork.domain.interaction.dto.response.CompanyInteractionResponse;
import com.gittowork.domain.interaction.service.CompanyInteractionService;
import com.gittowork.global.dto.response.ApiResponse;
import com.gittowork.global.dto.response.MessageOnlyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
@Tag(name = "Company Interaction", description = "유저-회사 상호작용 관련 API")
@Validated
public class CompanyInteractionController {
    private final CompanyInteractionService companyInteractionService;

    @Operation(
            summary = "스크랩한 회사 목록 조회",
            description = "현재 인증된 사용자가 스크랩한 회사 목록을 페이지네이션하여 조회합니다.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = CompanyInteractionResponse.class)))
            }
    )
    @GetMapping("/scrap")
    public ApiResponse<CompanyInteractionResponse> getScrapCompany(
            @Parameter(description = "페이지 번호 (0부터 시작)", required = true, example = "0")
            @RequestParam Integer page,
            @Parameter(description = "페이지 크기", required = true, example = "10")
            @RequestParam Integer size
    ) {
        return ApiResponse.success(companyInteractionService.getScrapCompany(page, size));
    }

    @Operation(
            summary = "회사 스크랩 추가",
            description = "현재 인증된 사용자가 특정 회사를 스크랩 목록에 추가합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "추가할 회사 ID를 포함한 요청 바디",
                    required = true,
                    content = @Content(schema = @Schema(implementation = InteractionPostRequest.class))
            ),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추가 성공",
                            content = @Content(schema = @Schema(implementation = MessageOnlyResponse.class)))
            }
    )
    @PostMapping("/scrap")
    public ApiResponse<MessageOnlyResponse> addScrapCompany(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "companyId 필드 포함 JSON 요청", required = true)
            @RequestBody InteractionPostRequest interactionPostRequest
    ) {
        return ApiResponse.success(
                companyInteractionService.addScrapCompany(interactionPostRequest.getCompanyId())
        );
    }

    @Operation(
            summary = "회사 스크랩 삭제",
            description = "현재 인증된 사용자의 스크랩 목록에서 특정 회사를 삭제합니다.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공",
                            content = @Content(schema = @Schema(implementation = MessageOnlyResponse.class)))
            }
    )
    @DeleteMapping("/scrap")
    public ApiResponse<MessageOnlyResponse> deleteScrapCompany(
            @Parameter(description = "삭제할 회사 ID", required = true, example = "123")
            @RequestParam Integer companyId
    ) {
        return ApiResponse.success(companyInteractionService.deleteScrapCompany(companyId));
    }

    @Operation(
            summary = "좋아요한 회사 목록 조회",
            description = "현재 인증된 사용자가 좋아요한 회사 목록을 페이지네이션하여 조회합니다.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = CompanyInteractionResponse.class)))
            }
    )
    @GetMapping("/like")
    public ApiResponse<CompanyInteractionResponse> getMyLikeCompany(
            @Parameter(description = "페이지 번호 (0부터 시작)", required = true, example = "0")
            @RequestParam Integer page,
            @Parameter(description = "페이지 크기", required = true, example = "10")
            @RequestParam Integer size
    ) {
        return ApiResponse.success(companyInteractionService.getMyLikeCompany(page, size));
    }

    @Operation(
            summary = "회사 좋아요 추가",
            description = "현재 인증된 사용자가 특정 회사를 좋아요 목록에 추가합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "추가할 회사 ID를 포함한 요청 바디",
                    required = true,
                    content = @Content(schema = @Schema(implementation = InteractionPostRequest.class))
            ),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추가 성공",
                            content = @Content(schema = @Schema(implementation = MessageOnlyResponse.class)))
            }
    )
    @PostMapping("/like")
    public ApiResponse<MessageOnlyResponse> addLikeCompany(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "companyId 필드 포함 JSON 요청", required = true)
            @RequestBody InteractionPostRequest interactionPostRequest
    ) {
        return ApiResponse.success(
                companyInteractionService.addLikeCompany(interactionPostRequest.getCompanyId())
        );
    }

    @Operation(
            summary = "회사 좋아요 삭제",
            description = "현재 인증된 사용자의 좋아요 목록에서 특정 회사를 삭제합니다.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공",
                            content = @Content(schema = @Schema(implementation = MessageOnlyResponse.class)))
            }
    )
    @DeleteMapping("/like")
    public ApiResponse<MessageOnlyResponse> deleteLikeCompany(
            @Parameter(description = "삭제할 회사 ID", required = true, example = "123")
            @RequestParam Integer companyId
    ) {
        return ApiResponse.success(companyInteractionService.deleteLikeCompany(companyId));
    }

    @Operation(
            summary = "블랙리스트 회사 목록 조회",
            description = "현재 인증된 사용자가 블랙리스트에 등록한 회사 목록을 페이지네이션하여 조회합니다.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = CompanyInteractionResponse.class)))
            }
    )
    @GetMapping("/blacklist")
    public ApiResponse<CompanyInteractionResponse> getMyBlackList(
            @Parameter(description = "페이지 번호 (0부터 시작)", required = true, example = "0")
            @RequestParam Integer page,
            @Parameter(description = "페이지 크기", required = true, example = "10")
            @RequestParam Integer size
    ) {
        return ApiResponse.success(companyInteractionService.getMyBlackList(page, size));
    }

    @Operation(
            summary = "블랙리스트 회사 추가",
            description = "현재 인증된 사용자가 특정 회사를 블랙리스트에 추가합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "추가할 회사 ID를 포함한 요청 바디",
                    required = true,
                    content = @Content(schema = @Schema(implementation = InteractionPostRequest.class))
            ),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추가 성공",
                            content = @Content(schema = @Schema(implementation = MessageOnlyResponse.class)))
            }
    )
    @PostMapping("/blacklist")
    public ApiResponse<MessageOnlyResponse> addMyBlackList(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "companyId 필드 포함 JSON 요청", required = true)
            @RequestBody InteractionPostRequest interactionPostRequest
    ) {
        return ApiResponse.success(
                companyInteractionService.addMyBlackList(interactionPostRequest.getCompanyId())
        );
    }

    @Operation(
            summary = "블랙리스트 회사 삭제",
            description = "현재 인증된 사용자의 블랙리스트에서 특정 회사를 삭제합니다.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공",
                            content = @Content(schema = @Schema(implementation = MessageOnlyResponse.class)))
            }
    )
    @DeleteMapping("/blacklist")
    public ApiResponse<MessageOnlyResponse> deleteMyBlackList(
            @Parameter(description = "삭제할 회사 ID", required = true, example = "123")
            @RequestParam Integer companyId
    ) {
        return ApiResponse.success(companyInteractionService.deleteMyBlackList(companyId));
    }
}
