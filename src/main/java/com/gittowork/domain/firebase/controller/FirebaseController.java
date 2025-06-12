package com.gittowork.domain.firebase.controller;

import com.gittowork.domain.firebase.dto.request.GetTokenRequest;
import com.gittowork.domain.firebase.service.FirebaseService;
import com.gittowork.global.response.ApiResponse;
import com.gittowork.global.response.MessageOnlyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/firebase")
@RequiredArgsConstructor
public class FirebaseController {

    private final FirebaseService firebaseService;

    @PostMapping("/create/fcm-token")
    public ApiResponse<MessageOnlyResponse> insertFcmToken(@RequestBody GetTokenRequest getTokenRequest) {
        return ApiResponse.success(HttpStatus.OK, firebaseService.insertFcmToken(getTokenRequest));
    }
}
