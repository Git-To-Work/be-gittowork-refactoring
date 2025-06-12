package com.gittowork.domain.authentication.service;

import com.gittowork.domain.authentication.dto.response.AutoLogInGithubResponse;
import com.gittowork.domain.authentication.dto.response.LogOutGithubResponse;
import com.gittowork.domain.authentication.dto.response.SignInGithubResponse;
import com.gittowork.domain.user.entity.User;
import com.gittowork.domain.user.entity.UserGitInfo;
import com.gittowork.domain.user.repository.UserGitInfoRepository;
import com.gittowork.domain.user.repository.UserRepository;
import com.gittowork.global.exception.AccessTokenNotFoundException;
import com.gittowork.global.exception.AutoLogInException;
import com.gittowork.global.exception.GithubSignInException;
import com.gittowork.global.exception.UserNotFoundException;
import com.gittowork.global.service.GithubRestApiService;
import com.gittowork.global.service.RedisService;
import com.gittowork.global.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class GithubAuthenticationService {

    private final GithubRestApiService githubRestApiService;


    private final UserRepository userRepository;
    private final UserGitInfoRepository userGitInfoRepository;
    private final RedisService redisService;
    private final JwtUtil jwtUtil;

    @Autowired
    public GithubAuthenticationService(UserRepository userRepository,
                                       RedisService redisService,
                                       UserGitInfoRepository userGitInfoRepository,
                                       JwtUtil jwtUtil, GithubRestApiService githubRestApiService) {
        this.userRepository = userRepository;
        this.userGitInfoRepository = userGitInfoRepository;
        this.redisService = redisService;
        this.jwtUtil = jwtUtil;
        this.githubRestApiService = githubRestApiService;
    }

    /**
     * 1. 메서드 설명: GitHub OAuth 코드를 사용하여 액세스 토큰을 요청한다.
     * 2. 로직:
     *    - RestTemplate 및 HttpHeaders(JSON 응답) 생성
     *    - 요청 본문에 client_id, client_secret, redirect_uri, code 추가
     *    - GitHub의 액세스 토큰 URL에 POST 요청 전송
     *    - 응답에서 access_token 추출, 없으면 예외 발생
     * 3. param: code - GitHub OAuth 인증 후 전달받은 코드
     * 4. return: GitHub 액세스 토큰 문자열
     */
    private String getAccessToken(String code) {
        Map<String, Object> responseBody = githubRestApiService.getAccessToken(code);

        log.info(responseBody.toString());

        if (!responseBody.containsKey("access_token")) {
            throw new GithubSignInException("Unauthorized or Invalid Code.");
        }

        return responseBody.get("access_token").toString();
    }

    /**
     * 1. 메서드 설명: GitHub 액세스 토큰을 사용하여 사용자 정보를 조회한다.
     * 2. 로직:
     *    - RestTemplate 및 HttpHeaders(Bearer 인증, JSON 응답) 생성
     *    - GitHub 사용자 정보 URL에 GET 요청 전송
     *    - 응답에서 사용자 정보 Map 반환
     * 3. param: accessToken - GitHub 액세스 토큰
     * 4. return: GitHub 사용자 정보가 담긴 Map
     */
    private Map<String, Object> getUserInfo(String accessToken) {
        Map<String, Object> userInfo = githubRestApiService.getUserInfo(accessToken);

        if (userInfo == null || !userInfo.containsKey("login")) {
            throw new GithubSignInException("Failed to fetch GitHub user info.");
        }

        return userInfo;
    }

    /**
     * 1. 메서드 설명: GitHub OAuth를 통해 로그인 후 사용자 정보를 저장하고 응답 객체를 생성한다.
     * 2. 로직:
     *    - GitHub 액세스 토큰을 요청하고 사용자 정보를 조회한다.
     *    - GitHub 관련 추가 정보와 사용자 기본 정보를 분리하여 Map에 저장한다.
     *    - Redis에 사용자 정보와 GitHub 추가 정보를 각각 저장한다.
     *    - SignInGithubResponse 빌더를 통해 응답 객체를 생성한다.
     * 3. param: code - GitHub OAuth 인증 후 받은 코드
     * 4. return: SignInGithubResponse 객체 (nickname, privacyPolicyAgreed, avatarUrl, accessToken 포함)
     */
    @Transactional
    public SignInGithubResponse signInGithub(String code) {
        String githubAccessToken = getAccessToken(code);
        Map<String, Object> githubUserInfo = getUserInfo(githubAccessToken);

        final String username = (String) githubUserInfo.get("login");

        Optional<User> loginUser = userRepository.findByGithubName(username);

        if (loginUser.isPresent()) {
            setAuthentication(loginUser.get().getGithubName());
            String accessToken = getAccessTokenAndStoreRefreshToken(loginUser.get().getGithubName());

            loginUser.get().setGithubAccessToken(githubAccessToken);

            userRepository.save(loginUser.get());

            return SignInGithubResponse.builder()
                    .nickname(loginUser.get().getGithubName())
                    .privacyPolicyAgreed(loginUser.get().getPrivacyConsentDttm() != null)
                    .avatarUrl(loginUser.get().getUserGitInfo().getAvatarUrl())
                    .accessToken(accessToken)
                    .build();
        }

        Map<String, Object> userGitInfo = new HashMap<>();
        userGitInfo.put("githubAvatarUrl", githubUserInfo.get("avatar_url"));
        userGitInfo.put("publicRepositories", githubUserInfo.get("public_repos"));
        userGitInfo.put("followers", githubUserInfo.get("followers"));
        userGitInfo.put("following", githubUserInfo.get("following"));

        Map<String, Object> user = new HashMap<>();
        user.put("githubId", githubUserInfo.get("id"));
        user.put("githubName", username);
        user.put("githubEmail", githubUserInfo.get("email"));
        user.put("githubAccessToken", githubAccessToken);

        String userKey = "user: " + username;
        String userGitInfoKey = "userGitInfo: " + username;

        redisService.saveUser(userKey, user);
        redisService.saveUserGitInfo(userGitInfoKey, userGitInfo);
        redisService.setExpire(userKey, 1, TimeUnit.HOURS);
        redisService.setExpire(userGitInfoKey, 1, TimeUnit.HOURS);

        setAuthentication(username);
        String accessToken = getAccessTokenAndStoreRefreshToken(username);

        return SignInGithubResponse.builder()
                .nickname(username)
                .privacyPolicyAgreed(false)
                .avatarUrl((String) githubUserInfo.get("avatar_url"))
                .accessToken(accessToken)
                .build();
    }

    /**
     * 1. 메서드 설명: 제공된 username을 기반으로 ROLE_USER 권한을 가진 UsernamePasswordAuthenticationToken을 생성하여
     *    Spring SecurityContext에 설정하는 메서드.
     * 2. 로직:
     *    - 주어진 username과 ROLE_USER 권한으로 UsernamePasswordAuthenticationToken 객체를 생성한다.
     *    - 생성된 토큰을 SecurityContextHolder에 설정하여 현재 인증 정보를 업데이트한다.
     * 3. param:
     *      - username: 인증할 사용자의 이름.
     * 4. return: 없음.
     */
    private void setAuthentication(String username) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }

    /**
     * 1. 메서드 설명: 제공된 username에 대해 JWT 액세스 토큰과 리프레시 토큰을 생성하고, 생성된 리프레시 토큰을 Redis에 저장한 후,
     *    생성된 액세스 토큰을 반환하는 메서드.
     * 2. 로직:
     *    - jwtUtil.generateAccessToken()을 사용하여 JWT 액세스 토큰을 생성한다.
     *    - jwtUtil.generateRefreshToken()을 사용하여 JWT 리프레시 토큰을 생성한다.
     *    - username을 기반으로 리프레시 토큰을 저장할 Redis 키를 생성한다.
     *    - redisService를 사용하여 생성된 리프레시 토큰을 366일 동안 저장한다.
     *    - 생성된 액세스 토큰을 반환한다.
     * 3. param:
     *      - username: 토큰을 생성할 사용자의 이름.
     * 4. return: 생성된 JWT 액세스 토큰 (String).
     */
    private String getAccessTokenAndStoreRefreshToken(String username) {
        String accessToken = jwtUtil.generateAccessToken(username);
        String refreshToken = jwtUtil.generateRefreshToken(username);
        String refreshTokenKey = username + "_refresh_token";
        redisService.saveRefreshToken(refreshTokenKey, refreshToken, 366, TimeUnit.DAYS);
        return accessToken;
    }

    /**
     * 1. 메서드 설명: JWT 기반 자동 로그인 시, SecurityContext의 인증 정보를 활용해 사용자 정보를 조회하고 응답 객체를 생성한다.
     * 2. 로직:
     *    - SecurityContextHolder에서 Authentication 객체를 가져온다.
     *    - 인증 정보가 없거나 인증되지 않은 경우 예외를 발생시킨다.
     *    - 인증된 사용자 닉네임을 기반으로 user와 userGitInfo를 조회한다.
     *    - 조회된 정보를 바탕으로 AutoLogInGithubResponse 객체를 빌더 패턴으로 생성한다.
     * 3. param: 없음
     * 4. return: AutoLogInGithubResponse 객체 (닉네임, 개인정보 동의 여부, 아바타 URL 포함)
     */
    @Transactional
    public AutoLogInGithubResponse autoLogInGithub() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new AutoLogInException("Unauthorized User.");
        }

        String username = authentication.getName();

        User user = userRepository.findByGithubName(username)
                .orElseThrow(() -> new AutoLogInException("User not found."));
        UserGitInfo userGitInfo = userGitInfoRepository.findById(user.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        return AutoLogInGithubResponse.builder()
                .nickname(username)
                .privacyPolicyAgreed(user.getPrivacyConsentDttm() != null)
                .avatarUrl(userGitInfo.getAvatarUrl())
                .build();
    }

    /**
     * 1. 메서드 설명: 현재 요청 헤더에 담긴 access token을 추출하여 블랙리스트에 등록하고, 저장된 refresh token을 삭제한다.
     * 2. 로직:
     *    - RequestContextHolder를 통해 현재 HttpServletRequest를 가져와 Authorization 헤더에서 bearer token을 추출한다.
     *    - bearer token에서 "Bearer " 접두어를 제거하여 access token을 얻는다.
     *    - JwtTokenProvider를 사용해 access token의 남은 유효 시간을 계산하고, RedisService를 통해 해당 토큰을 블랙리스트에 등록한다.
     *    - SecurityContext에서 인증 정보를 조회하여 username을 추출한 후, 해당 refresh token 키를 사용해 Redis에서 삭제한다.
     * 3. param: 없음 (access token은 현재 요청 헤더에서 추출)
     * 4. return: 없음
     */
    public LogOutGithubResponse logout() {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new AccessTokenNotFoundException("Access token not found.");
        }
        String accessToken = bearerToken.substring(7);

        long remainingTimeMillis = jwtUtil.getRemainExpiredTime(accessToken);
        redisService.addTokenToBlacklist(accessToken, remainingTimeMillis, TimeUnit.MILLISECONDS);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        String refreshTokenKey = username + "_refresh_token";
        redisService.deleteKey(refreshTokenKey);

        return LogOutGithubResponse.builder()
                .message("Successfully logged out.")
                .build();
    }
}
