package com.gittowork.domain.authentication.service;

import com.gittowork.domain.user.entity.User;
import com.gittowork.domain.user.entity.UserGitInfo;
import com.gittowork.domain.user.entity.UserState;
import com.gittowork.domain.user.repository.UserGitInfoRepository;
import com.gittowork.domain.user.repository.UserRepository;
import com.gittowork.global.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

/**
 * Spring Security OAuth2 클라이언트를 활용해 GitHub 사용자 정보를 조회하고
 * 사용자 엔티티를 생성·업데이트하며, 인증 컨텍스트를 설정하는 서비스.
 * <p>
 * - 신규 사용자는 ONBOARDING 상태로 User 및 UserGitInfo 엔티티를 생성
 * - 기존 사용자는 Access Token을 갱신
 * - 인증 상태에 따라 ROLE_USER 또는 ROLE_ONBOARDING 권한 부여
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final UserGitInfoRepository userGitInfoRepository;

    /**
     * GitHub OAuth2 인증 후 사용자 정보를 로드하고, 애플리케이션 사용자 정보를 생성 또는 갱신한 뒤
     * Spring Security 인증 컨텍스트에 설정합니다.
     *
     * @param userRequest OAuth2UserRequest 객체로, GitHub로부터 전달된 인가 코드와 클라이언트 정보를 포함
     * @return OAuth2User 인증된 사용자의 상세 정보를 담은 DefaultOAuth2User
     * @throws OAuth2AuthenticationException GitHub API 호출 실패 시 발생
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        Map<String, Object> attrs = oauth2User.getAttributes();
        String username = (String) attrs.get("login");
        String githubAccessToken = userRequest.getAccessToken().getTokenValue();

        User user = userRepository.findByGithubName(username)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .githubId(((Number) attrs.get("id")).intValue())
                            .githubName(username)
                            .githubEmail((String) attrs.get("email"))
                            .githubAccessToken(githubAccessToken)
                            .state(UserState.ONBOARDING)
                            .createDttm(LocalDateTime.now())
                            .updateDttm(LocalDateTime.now())
                            .build();
                    newUser = userRepository.save(newUser);

                    UserGitInfo userGitInfo = UserGitInfo.builder()
                            .user(newUser)
                            .avatarUrl((String) attrs.get("avatar_url"))
                            .publicRepositories((Integer) attrs.get("public_repos"))
                            .followers((Integer) attrs.get("followers"))
                            .followings((Integer) attrs.get("following"))
                            .createDttm(LocalDateTime.now())
                            .updateDttm(LocalDateTime.now())
                            .build();
                    userGitInfoRepository.save(userGitInfo);

                    return newUser;
                });

        user.setGithubAccessToken(githubAccessToken);
        userRepository.save(user);

        String role = (user.getState() == UserState.ACTIVE)
                ? "ROLE_USER" : "ROLE_ONBOARDING";

        DefaultOAuth2User principal = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(role)),
                attrs,
                "login"
        );

        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities()
                        )
                );

        return principal;
    }
}
