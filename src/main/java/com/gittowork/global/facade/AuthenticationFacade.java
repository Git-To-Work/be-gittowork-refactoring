package com.gittowork.global.facade;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFacade {

    /**
     * 현재 SecurityContext에 설정된 Authentication에서 username(subject)를 반환합니다.
     * @return 인증된 사용자명
     * @throws AuthenticationCredentialsNotFoundException 인증 정보가 없을 경우
     */
    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("No authenticated user");
        }
        return auth.getName();
    }
}
