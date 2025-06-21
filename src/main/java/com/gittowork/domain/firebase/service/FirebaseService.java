package com.gittowork.domain.firebase.service;

import com.gittowork.domain.firebase.dto.request.TokenRequest;
import com.gittowork.domain.firebase.entity.UserAlertLog;
import com.gittowork.domain.firebase.repository.UserAlertLogRepository;
import com.gittowork.domain.user.entity.User;
import com.gittowork.domain.user.repository.UserRepository;
import com.gittowork.global.exception.auth.UserNotFoundException;
import com.gittowork.global.dto.response.MessageOnlyResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Firebase 관련 기능을 제공하는 서비스 클래스입니다.
 * <p>
 * - FCM 토큰 등록 및 업데이트
 * - 커버레터 및 GitHub 분석 알림 메시지 전송
 * - 알림 로그 저장
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FirebaseService {

    private final UserAlertLogRepository userAlertLogRepository;
    private final UserRepository userRepository;

    /**
     * 사용자의 FCM 토큰을 DB에 저장 또는 업데이트합니다.
     *
     * @param request 클라이언트로부터 전달된 FCM 토큰 정보 DTO
     * @return 성공 메시지를 담은 {@link MessageOnlyResponse}
     * @throws UserNotFoundException 인증된 사용자 정보를 찾을 수 없을 때 발생
     */
    public MessageOnlyResponse insertFcmToken(TokenRequest request) {
        // 현재 인증된 사용자명 조회
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();

        // FCM 토큰 업데이트 쿼리 실행
        int updated = userRepository.updateFcmTokenByGithubName(userName, request.getFcmToken());
        if (updated == 0) {
            throw new UserNotFoundException(userName);
        }

        return MessageOnlyResponse.builder()
                .message("FCM 토큰이 성공적으로 저장되었습니다.")
                .build();
    }

    /**
     * 커버레터 분석 완료 알림 메시지를 FCM으로 전송하고,
     * 성공 시 알림 로그를 저장합니다.
     *
     * @param user 사용자 엔티티 (토큰 저장 정보 포함)
     * @param title 알림 제목
     * @param message 알림 본문 메시지
     * @param alertType 알림 유형 식별자
     * @throws FirebaseMessagingException FCM 전송 중 오류가 발생할 때 던져집니다.
     */
    public void sendCoverLetterMessage(User user, String title, String message, String alertType) throws FirebaseMessagingException {
        FirebaseMessaging.getInstance().send(
                Message.builder()
                        .putData("title", title)
                        .putData("body", message)
                        .setToken(user.getFcmToken())
                        .build()
        );

        userAlertLogRepository.save(
                UserAlertLog.builder()
                        .alertType(alertType)
                        .user(user)
                        .title(title)
                        .message(message)
                        .build()
        );
    }

    /**
     * GitHub 분석 완료 알림 메시지를 FCM으로 전송하고,
     * 성공 시 알림 로그를 저장합니다.
     *
     * @param user 사용자 엔티티 (토큰 저장 정보 포함)
     * @param title 알림 제목
     * @param message 알림 본문 메시지
     * @param alertType 알림 유형 식별자
     * @param selectedRepositoryId 분석된 GitHub 저장소 ID
     * @throws FirebaseMessagingException FCM 전송 중 오류가 발생할 때 던져집니다.
     */
    public void sendGithubAnalysisMessage(User user, String title, String message, String alertType, String selectedRepositoryId) throws FirebaseMessagingException {
        FirebaseMessaging.getInstance().send(
                Message.builder()
                        .putData("title", title)
                        .putData("body", message)
                        .putData("selectedRepositoryId", selectedRepositoryId)
                        .setToken(user.getFcmToken())
                        .build()
        );

        userAlertLogRepository.save(
                UserAlertLog.builder()
                        .alertType(alertType)
                        .user(user)
                        .title(title)
                        .message(message)
                        .build()
        );
    }
}
