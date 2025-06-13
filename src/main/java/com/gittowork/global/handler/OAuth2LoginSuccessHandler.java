package com.gittowork.global.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gittowork.domain.authentication.dto.response.SignInGithubResponse;
import com.gittowork.domain.user.entity.User;
import com.gittowork.domain.user.entity.UserState;
import com.gittowork.domain.user.repository.UserRepository;
import com.gittowork.global.exception.UserNotFoundException;
import com.gittowork.global.response.ApiResponse;
import com.gittowork.global.service.RedisService;
import com.gittowork.global.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * OAuth2 로그인 성공 시 호출되는 핸들러 클래스.
 * <p>
 * GitHub OAuth 인증이 완료되면 이 핸들러가 실행되어,
 * 사용자 상태(UserState)에 따라 Access Token 또는 온보딩 토큰을 생성하고,
 * 활성 사용자에게는 Refresh Token을 발급하여 Redis에 저장합니다.
 * <p>
 * 생성된 토큰 및 사용자 정보를 SignInGithubResponse DTO에 담아 ApiResponse로 감싸서
 * JSON 형태로 클라이언트에 반환합니다.
 */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final RedisService redisService;

    /**
     * 인증 성공 후 실행되는 메서드.
     * <ol>
     *   <li>OAuth2User에서 GitHub 로그인명(username) 추출</li>
     *   <li>UserRepository에서 사용자 조회(없으면 예외 발생)</li>
     *   <li>UserState가 ACTIVE인 경우 일반 Access Token과 Refresh Token 발급;</li>
     *   <li>그렇지 않은 경우 온보딩 전용 토큰 발급</li>
     *   <li>SignInGithubResponse DTO에 사용자 정보와 토큰 정보를 설정</li>
     *   <li>ApiResponse로 래핑하여 JSON 응답</li>
     * </ol>
     *
     * @param request        HTTP 요청 객체
     * @param response       HTTP 응답 객체
     * @param authentication OAuth2 인증 정보가 담긴 Authentication 객체
     * @throws IOException 응답 작성 중 I/O 오류 발생 시
     */
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String username = oAuth2User.getAttribute("login");
        User user = userRepository.findByGithubName(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        boolean isActive = user.getState() == UserState.ACTIVE;

        String accessToken = isActive
                ? jwtUtil.generateAccessToken(username)
                : jwtUtil.generateOnboardingToken(username);

        String refreshToken = null;
        if (isActive) {
            refreshToken = jwtUtil.generateRefreshToken(username);
            redisService.saveRefreshToken(username + "_refresh_token", refreshToken, 366, TimeUnit.DAYS);
        }

        SignInGithubResponse dto = SignInGithubResponse.builder()
                .nickname(username)
                .accessToken(accessToken)
                .privacyPolicyAgreed(user.getPrivacyConsentDttm() != null)
                .avatarUrl(user.getUserGitInfo().getAvatarUrl())
                .onboarding(!isActive)
                .build();

        ApiResponse<SignInGithubResponse> apiResponse =
                ApiResponse.success(HttpStatus.OK, dto);
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), apiResponse);
    }
}
