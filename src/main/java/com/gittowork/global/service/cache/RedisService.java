package com.gittowork.global.service.cache;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * userGitInfo 저장
     *
     * @param key          Redis에 저장할 키
     * @param userGitInfo  저장할 GitHub 관련 추가 정보 Map
     */
    public void saveUserGitInfo(String key, Map<String, Object> userGitInfo) {
        redisTemplate.opsForHash().putAll(key, userGitInfo);
    }

    /**
     * user 저장
     *
     * @param key   Redis에 저장할 키
     * @param user  저장할 사용자 기본 정보 Map
     */
    public void saveUser(String key, Map<String, Object> user) {
        redisTemplate.opsForHash().putAll(key, user);
    }

    /**
     * refreshToken 저장
     *
     * @param key   Redis에 저장할 키
     * @param refreshToken  저장할 refresh token
     */
    public void saveRefreshToken(String key, String refreshToken, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, refreshToken, timeout, unit);
    }

    /**
     * Redis 키의 만료 시간을 설정한다.
     *
     * @param key      만료 시간을 설정할 Redis 키
     * @param timeout  만료 시간 (예: 1)
     * @param unit     시간 단위 (예: TimeUnit.HOURS)
     */
    public void setExpire(String key, long timeout, TimeUnit unit) {
        redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 1. 메서드 설명: access token을 Redis 블랙리스트에 추가하고, 주어진 만료 시간 동안 유지한다.
     * 2. 로직:
     *    - access token에 "blacklist:" 접두사를 붙여 key를 생성한다.
     *    - Redis에 key와 "true" 값을 저장하며, 만료 시간을 설정한다.
     * 3. param:
     *    - token: 블랙리스트에 추가할 access token
     *    - timeout: 토큰의 남은 유효 시간을 나타내는 값
     *    - unit: timeout의 시간 단위
     * 4. return: 없음
     */
    public void addTokenToBlacklist(String token, long timeout, TimeUnit unit) {
        String key = "blacklist:" + token;
        redisTemplate.opsForValue().set(key, "true", timeout, unit);
    }

    /**
     * 1. 메서드 설명: Redis에서 지정된 키를 삭제한다.
     * 2. 로직:
     * - RedisTemplate의 delete 메서드를 호출하여 key를 삭제한다.
     * 3. param: key - 삭제할 Redis 키
     * 4. return: 삭제 여부(boolean)
     */
    public void deleteKey(String key) {
        redisTemplate.delete(key);
    }

}
