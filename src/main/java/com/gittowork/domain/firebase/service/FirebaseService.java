package com.gittowork.domain.firebase.service;

import com.gittowork.domain.firebase.dto.request.GetTokenRequest;
import com.gittowork.domain.firebase.entity.UserAlertLog;
import com.gittowork.domain.firebase.repository.UserAlertLogRepository;
import com.gittowork.domain.user.entity.User;
import com.gittowork.domain.user.repository.UserRepository;
import com.gittowork.global.exception.UserNotFoundException;
import com.gittowork.global.response.MessageOnlyResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class FirebaseService {

    private final UserAlertLogRepository userAlertLogRepository;
    private final UserRepository userRepository;

    /**
     * 1. 메서드 설명 :
     *      - 현재 인증된 사용자의 FCM 토큰을 업데이트하여 저장합니다.
     * 2. 로직:
     *      - SecurityContextHolder에서 현재 인증된 사용자의 이름을 조회합니다.
     *      - userRepository를 통해 해당 사용자를 조회하고, 존재하지 않으면 UserNotFoundException을 발생시킵니다.
     *      - 사용자의 FCM 토큰을 업데이트하고 저장합니다.
     *      - 성공 메시지가 포함된 MessageOnlyResponse 객체를 반환합니다.
     * 3. param:
     *      - getTokenRequest : 사용자의 FCM 토큰 정보를 포함하는 객체.
     * 4. return:
     *      - FCM 토큰이 성공적으로 저장되었음을 알리는 MessageOnlyResponse 객체.
     * 5. 예외:
     *      - UserNotFoundException : 사용자 조회에 실패할 경우 발생.
     */
    @Transactional
    public MessageOnlyResponse insertFcmToken(GetTokenRequest getTokenRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();

        User user = userRepository.findByGithubName(userName)
                .orElseThrow(() -> new UserNotFoundException(userName));

        user.setFcmToken(getTokenRequest.getFcmToken());

        userRepository.save(user);

        return MessageOnlyResponse.builder()
                .message("FCM 토큰이 성공적으로 저장되었습니다.")
                .build();
    }

    /**
     * 1. 메서드 설명 :
     *      - 주어진 사용자에게 푸시 알림 메시지를 전송하고, 전송 내역을 사용자 알림 로그에 기록합니다.
     * 2. 로직:
     *      - FirebaseMessaging API를 사용하여 주어진 title과 message를 포함한 푸시 알림 메시지를 전송합니다.
     *      - 전송된 메시지의 결과를 로그에 기록합니다.
     *      - 전송 내역을 userAlertLogRepository에 저장하여 알림 로그를 남깁니다.
     * 3. param:
     *      - user : 메시지를 받을 대상 사용자.
     *      - title : 푸시 알림의 제목.
     *      - message : 푸시 알림의 본문 메시지.
     *      - alertType : 알림의 유형을 나타내는 문자열.
     * 4. return:
     *      - 없음.
     * 5. 예외:
     *      - FirebaseMessagingException : 메시지 전송에 실패할 경우 발생.
     */
    @Transactional
    public void sendCoverLetterMessage(User user, String title, String message, String alertType) throws FirebaseMessagingException {
        String firebaseMessage = FirebaseMessaging.getInstance().send(
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
                        .createDttm(LocalDateTime.now())
                        .build()
        );

        log.info("Firebase send message: {}", firebaseMessage);
    }

    @Transactional
    public void sendGithubAnalysisMessage(User user, String title, String message, String alertType, String selectedRepositoryId) throws FirebaseMessagingException {
        String firebaseMessage = FirebaseMessaging.getInstance().send(
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
                        .createDttm(LocalDateTime.now())
                        .build()
        );

        log.info("Firebase send message: {}", firebaseMessage);
    }

}
