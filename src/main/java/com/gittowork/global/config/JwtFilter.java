package com.gittowork.global.config;

import com.gittowork.global.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public JwtFilter(JwtUtil jwtUtil, RedisTemplate<String, Object> redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    //필터 제외 목록
    private static final List<String> EXCLUDED_PATHS = List.of(
            //swagger 관련 경로
            "/swagger-ui/",
            "/v3/api-docs",
            "/swagger-resources",
            "/webjars",

            //postman 관련 경로
            "/api/**"
    );

    /**
     * 1. 메서드 설명: HTTP 요청마다 JWT 토큰을 추출 및 검증하여, 유효한 토큰이 있을 경우 해당 사용자의 인증 정보를 SecurityContext에 설정한다.
     * 2. 로직:
     *    - 요청 URI가 EXCLUDED_PATHS에 해당하면, JWT 검증 없이 필터 체인을 통해 다음 필터로 바로 전달한다.
     *    - "Authorization" 헤더에서 "Bearer " 접두어가 있는 경우 토큰을 추출한다.
     *    - JwtUtil을 사용해 토큰의 유효성을 검사하고, 유효하면 토큰에서 username을 추출한다.
     *    - (주석 처리된 부분) 필요 시 username을 기반으로 Redis에 저장된 refresh token의 존재 여부를 검사할 수 있다.
     *    - 추출된 username이 존재하며, 아직 SecurityContext에 인증 정보가 설정되지 않은 경우,
     *      UsernamePasswordAuthenticationToken을 생성하여 SecurityContext에 등록한다.
     * 3. param:
     *    - request: 현재 HTTP 요청 객체
     *    - response: 현재 HTTP 응답 객체
     *    - filterChain: 다음 필터로 요청과 응답을 전달하기 위한 FilterChain
     * 4. return: 없음. (필터 체인을 통해 요청 처리가 계속 진행된다.)
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        if (EXCLUDED_PATHS.stream().anyMatch(requestURI::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            if (jwtUtil.isValidToken(token)) {
                username = jwtUtil.getUsername(token);
            }
        }

        if (username != null) {
            String refreshTokenKey = username + "_refresh_token";

            if (!Boolean.TRUE.equals(redisTemplate.hasKey(refreshTokenKey))) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Refresh token not found. Please log in again.");
                return;
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);

    }
}
