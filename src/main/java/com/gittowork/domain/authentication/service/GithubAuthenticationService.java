package com.gittowork.domain.authentication.service;

import com.gittowork.domain.authentication.dto.response.AutoLogInGithubResponse;
import com.gittowork.domain.user.entity.User;
import com.gittowork.domain.user.entity.UserGitInfo;
import com.gittowork.domain.user.entity.UserState;
import com.gittowork.domain.user.repository.UserGitInfoRepository;
import com.gittowork.domain.user.repository.UserRepository;
import com.gittowork.global.exception.AccessTokenNotFoundException;
import com.gittowork.global.exception.AutoLogInException;
import com.gittowork.global.exception.UserNotFoundException;
import com.gittowork.global.service.RedisService;
import com.gittowork.global.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * GitHub OAuth 인증 및 세션 관리를 담당하는 서비스 클래스.
 * <p>
 * - 자동 로그인 (autoLogInGithub)
 * - 로그아웃 처리 (logout)
 */
@Service
@RequiredArgsConstructor
public class GithubAuthenticationService {

    private final UserRepository userRepository;
    private final UserGitInfoRepository userGitInfoRepository;
    private final RedisService redisService;
    private final JwtUtil jwtUtil;

    /**
     * JWT 기반 자동 로그인 처리.
     * <p>
     * 현재 SecurityContext 에 설정된 인증 정보를 확인하여,
     * 활성 상태(ACTIVE)인 사용자의 닉네임, 동의 여부, 아바타 URL 을 반환한다.
     *
     * @return AutoLogInGithubResponse 자동 로그인 시 반환할 사용자 정보
     * @throws AutoLogInException 인증 정보가 없거나 비활성 상태인 경우
     */
    @Transactional(readOnly = true)
    public AutoLogInGithubResponse autoLogInGithub() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new AutoLogInException("Unauthorized User.");
        }

        String username = auth.getName();
        User user = userRepository.findByGithubName(username)
                .orElseThrow(() -> new AutoLogInException("User not found."));

        if (user.getState() != UserState.ACTIVE) {
            throw new AutoLogInException("User not active.");
        }

        UserGitInfo gitInfo = user.getUserGitInfo();
        if (gitInfo == null) {
            throw new UserNotFoundException("GitHub info not found.");
        }

        return AutoLogInGithubResponse.builder()
                .nickname(username)
                .privacyPolicyAgreed(user.getPrivacyConsentDttm() != null)
                .avatarUrl(gitInfo.getAvatarUrl())
                .build();
    }

    /**
     * 로그아웃 처리.
     * <p>
     * - 제공된 accessToken 을 블랙리스트에 등록하여 더 이상 유효하지 않도록 하고,
     * - 현재 인증된 사용자의 refreshToken 키를 삭제한다.
     *
     * @param accessToken Bearer 접두어가 제거된 순수 accessToken 문자열
     * @throws AccessTokenNotFoundException accessToken 이 없을 경우
     */
    public void logout(String accessToken) {
        if (accessToken == null) {
            throw new AccessTokenNotFoundException("Access token not found.");
        }

        long remainingMillis = jwtUtil.getRemainExpiredTime(accessToken);
        redisService.addTokenToBlacklist(accessToken, remainingMillis, TimeUnit.MILLISECONDS);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        String refreshKey = username + "_refresh_token";
        redisService.deleteKey(refreshKey);

        SecurityContextHolder.clearContext();
    }
}
