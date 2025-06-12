package com.gittowork.domain.user.controller;

import com.gittowork.domain.user.dto.request.InsertProfileRequest;
import com.gittowork.domain.user.dto.request.UpdateInterestsFieldsRequest;
import com.gittowork.domain.user.dto.request.UpdateProfileRequest;
import com.gittowork.domain.user.dto.response.GetInterestFieldsResponse;
import com.gittowork.domain.user.dto.response.GetMyInterestFieldResponse;
import com.gittowork.domain.user.dto.response.GetMyProfileResponse;
import com.gittowork.domain.user.service.UserService;
import com.gittowork.global.response.ApiResponse;
import com.gittowork.global.response.MessageOnlyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@AllArgsConstructor(onConstructor = @__(@Autowired))
@Tag(name = "User", description = "유저 관련 CRUD")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Insert Profile", description = "개인 프로필 삽입")
    @PostMapping("/create/profile")
    public ApiResponse<MessageOnlyResponse> insertProfile(@NotNull @RequestBody InsertProfileRequest insertProfileRequest) {
        return ApiResponse.success(HttpStatus.OK, userService.insertProfile(insertProfileRequest));
    }

    @Operation(summary = "Get My Profile", description = "내 프로필 조회")
    @GetMapping("/select/profile")
    public ApiResponse<GetMyProfileResponse> getMyProfile() {
        return ApiResponse.success(HttpStatus.OK, userService.getMyProfile());
    }

    @Operation(summary = "Update Profile", description = "개인 프로필 수정")
    @PutMapping("/update/profile")
    public ApiResponse<MessageOnlyResponse> updateProfile(@NotNull @RequestBody UpdateProfileRequest updateProfileRequest) {
        return ApiResponse.success(HttpStatus.OK, userService.updateProfile(updateProfileRequest));
    }

    @Operation(summary = "Get Interest Field List", description = "관심분야 리스트 조회")
    @GetMapping("/select/interest-field-list")
    public ApiResponse<GetInterestFieldsResponse> getInterestFields() {
        return ApiResponse.success(HttpStatus.OK, userService.getInterestFields());
    }

    @Operation(summary = "Update Interest Field", description = "관심분야 수정")
    @PutMapping("/update/interest-field")
    public ApiResponse<MessageOnlyResponse> updateInterestField(@NotNull @RequestBody UpdateInterestsFieldsRequest updateInterestsFieldsRequest) {
        return ApiResponse.success(HttpStatus.OK, userService.updateInterestFields(updateInterestsFieldsRequest));
    }

    @Operation(summary = "Get My Interest Field", description = "내 관심분야 조회")
    @GetMapping("/select/my-interest-field")
    public ApiResponse<GetMyInterestFieldResponse> getMyInterestField() {
        return ApiResponse.success(HttpStatus.OK, userService.myInterestFields());
    }

    @Operation(summary = "Delete Account", description = "회원 탈퇴 처리")
    @DeleteMapping("/delete/account")
    public ApiResponse<MessageOnlyResponse> deleteAccount() {
        return ApiResponse.success(HttpStatus.OK, userService.deleteAccount());
    }
}
