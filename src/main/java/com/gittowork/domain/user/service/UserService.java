package com.gittowork.domain.user.service;

import com.gittowork.domain.field.entity.Field;
import com.gittowork.domain.field.repository.FieldRepository;
import com.gittowork.domain.github.service.GithubAnalysisService;
import com.gittowork.domain.user.dto.request.InsertProfileRequest;
import com.gittowork.domain.user.dto.request.UpdateInterestsFieldsRequest;
import com.gittowork.domain.user.dto.request.UpdateProfileRequest;
import com.gittowork.domain.user.dto.response.GetMyInterestFieldResponse;
import com.gittowork.domain.user.dto.response.GetMyProfileResponse;

import com.gittowork.domain.user.entity.User;
import com.gittowork.domain.user.repository.UserRepository;
import com.gittowork.global.exception.UserNotFoundException;
import com.gittowork.global.facade.AuthenticationFacade;
import com.gittowork.global.response.MessageOnlyResponse;
import com.gittowork.global.service.RedisService;
import com.gittowork.global.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RedisService redisService;
    private final FieldRepository fieldRepository;
    private final GithubAnalysisService githubAnalysisService;
    private final AuthenticationFacade authenticationFacade;

    private static final String USER_NOT_FOUND = "User not found";
    private final JwtUtil jwtUtil;

    /**
     * 프로필 추가 정보를 저장하는 비즈니스 로직.
     *
     * <p>1. 현재 인증된 사용자명(username)을 가져와 {@link UserRepository}에서 조회합니다.
     * <p>2. 조회된 엔티티에 요청 DTO의 값을 설정(update)하고,
     *    트랜잭션 커밋 시점에 더티체킹(dirty-checking)으로 자동 반영됩니다.
     * <p>3. 비동기로 GitHub 리포지토리 분석을 수행하는 {@link com.gittowork.domain.github.service.GithubAnalysisService}를 호출합니다.
     *
     * @param insertProfileRequest 프로필 추가 정보를 담은 DTO
     * @return {@link com.gittowork.global.response.MessageOnlyResponse}
     *         - message: \"추가 정보가 성공적으로 업데이트되었습니다.\"
     * @throws com.gittowork.global.exception.UserNotFoundException
     *         - 주어진 인증정보로 사용자를 찾을 수 없을 때
     */
    public MessageOnlyResponse insertProfile(InsertProfileRequest insertProfileRequest) {
        String username = authenticationFacade.getCurrentUsername();
        LocalDateTime now = LocalDateTime.now();

        User user = userRepository.findByGithubName(username)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));

        user.setName(insertProfileRequest.getName());
        user.setPhone(insertProfileRequest.getPhone());
        user.setBirthDt(insertProfileRequest.getBirthDt());
        user.setExperience(insertProfileRequest.getExperience());
        user.setPrivacyConsentDttm(insertProfileRequest.getPrivacyPolicyAgreed() ? now : null);
        user.setNotificationAgreeDttm(insertProfileRequest.getNotificationAgreed() ? now : null);

        githubAnalysisService.saveUserGithubRepositoryInfo(user.getGithubAccessToken(), username, user.getId());

        return MessageOnlyResponse.builder()
                .message("추가 정보가 성공적으로 업데이트되었습니다.")
                .build();
    }

    /**
     * 현재 인증된 사용자의 프로필 정보를 조회하여 반환합니다.
     *
     * <p>1. SecurityContext에서 사용자명(username)을 가져와 {@link UserRepository}에서 조회합니다.
     * <p>2. User 엔티티와 연관된 UserGitInfo를 통해 아바타 URL 등을 함께 조회합니다.
     * <p>3. 조회된 데이터를 {@link GetMyProfileResponse} DTO로 변환하여 반환합니다.
     *
     * @return GetMyProfileResponse 인증된 사용자의 프로필 정보
     * @throws com.gittowork.global.exception.UserNotFoundException
     *         - 인증 정보로 사용자를 찾지 못한 경우
     */
    @Transactional(readOnly = true)
    public GetMyProfileResponse getMyProfile() {
        String username = authenticationFacade.getCurrentUsername();

        User user = userRepository.findByGithubNameWithGitInfo(username)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));

        return GetMyProfileResponse.builder()
                .userId(user.getId())
                .email(user.getGithubEmail())
                .name(user.getName())
                .nickname(user.getGithubName())
                .phone(user.getPhone())
                .birthDt(user.getBirthDt())
                .experience(user.getExperience())
                .avatarUrl(user.getUserGitInfo().getAvatarUrl())
                .notificationAgreed(user.getNotificationAgreeDttm() != null)
                .build();
    }

    /**
     * 현재 인증된 사용자의 프로필 추가 정보를 수정하고 처리 결과를 반환합니다.
     *
     * <p>1. SecurityContext에서 인증된 사용자의 GitHub 로그인명(username)을 조회합니다.
     * <p>2. {@link UserRepository#findByGithubName(String)} 으로 {@link User} 엔티티를 조회합니다.
     *     - 조회 실패 시 {@link com.gittowork.global.exception.UserNotFoundException}을 던집니다.
     * <p>3. 요청 DTO의 각 필드(name, birthDt, experience, phone)로 {@code User} 엔티티를 업데이트하고,
     *     Dirty-Checking에 의해 커밋 시점에 변경사항이 반영됩니다.
     * <p>4. 알림 수신 동의 여부(notificationAgreeDttm)는 기존 동의 상태와 신규 동의를 비교하여
     *     변경이 있을 때에만 {@link java.time.LocalDateTime#now()} 또는 {@code null}로 설정합니다.
     * <p>5. 성공 시 메시지를 담은 {@link com.gittowork.global.response.MessageOnlyResponse}를 반환합니다.
     *
     * @param updateProfileRequest 사용자 프로필 수정 정보를 담은 DTO
     * @return MessageOnlyResponse  - message: "추가 정보 수정 요청 처리 완료"
     * @throws com.gittowork.global.exception.UserNotFoundException
     *         지정된 GitHub 사용자명을 가진 {@code User}가 존재하지 않을 경우
     */
    @Transactional
    public MessageOnlyResponse updateProfile(UpdateProfileRequest updateProfileRequest) {
        String username = authenticationFacade.getCurrentUsername();

        User user = userRepository.findByGithubName(username)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));

        user.setName(updateProfileRequest.getName());
        user.setBirthDt(updateProfileRequest.getBirthDt());
        user.setExperience(updateProfileRequest.getExperience());
        user.setPhone(updateProfileRequest.getPhone());

        boolean currentlyAgreed = user.getNotificationAgreeDttm() != null;
        boolean updateAgreed = updateProfileRequest.getNotificationAgreed();
        if (currentlyAgreed != updateAgreed) {
            user.setNotificationAgreeDttm(updateAgreed ? LocalDateTime.now() : null);
        }

        return MessageOnlyResponse.builder()
                .message("추가 정보 수정 요청 처리 완료")
                .build();
    }

    /**
     * 사용자 관심 분야 목록을 수정하고 저장합니다.
     *
     * <p>1. SecurityContext에서 인증된 사용자명(username)을 조회합니다.
     * <p>2. {@link UserRepository#findByGithubName(String)} 으로 {@link User} 엔티티를 조회합니다.
     *     - 조회 실패 시 {@link com.gittowork.global.exception.UserNotFoundException} 발생
     * <p>3. 요청 DTO의 {@code List<Integer> interestsFields}를 JSON 배열 형태의 문자열로 직렬화하여
     *     {@code User.interestFields} 필드에 저장합니다.
     * <p>4. Dirty‐Checking 호출을 통해 변경사항을 DB에 반영합니다.
     * <p>5. 처리 완료 메시지를 담은 {@link com.gittowork.global.response.MessageOnlyResponse}를 반환합니다.
     *
     * @param updateInterestsFieldsRequest 수정할 관심 분야의 ID 목록을 담은 DTO
     * @return MessageOnlyResponse  - message: "관심 비즈니스 분야 수정 처리 성공"
     * @throws com.gittowork.global.exception.UserNotFoundException
     *         지정된 GitHub 사용자명을 가진 {@code User}가 존재하지 않을 경우
     */
    public MessageOnlyResponse updateInterestFields(UpdateInterestsFieldsRequest updateInterestsFieldsRequest) {
        String username = authenticationFacade.getCurrentUsername();

        User user = userRepository.findByGithubName(username)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));

        String interestFields = updateInterestsFieldsRequest.getInterestsFields()
                .toString()
                .replaceAll("\\s+", "");

        user.setInterestFields(interestFields);

        return MessageOnlyResponse.builder()
                .message("관심 비즈니스 분야 수정 처리 성공")
                .build();
    }

    /**
     * 현재 인증된 사용자의 관심 비즈니스 분야 ID 및 이름 목록을 조회하여 응답 객체로 반환합니다.
     *
     * <p>1. SecurityContext에서 인증된 사용자명(username)을 조회합니다.
     * <p>2. {@link UserRepository#findByGithubName(String)}를 통해 {@link User} 엔티티를 로드합니다.
     *     - 조회 실패 시 {@link com.gittowork.global.exception.UserNotFoundException}이 발생합니다.
     * <p>3. 로드된 User의 {@code interestFields} 문자열을
     *     {@link #parseInterestFieldIds(String)} 메서드를 사용해 {@code List<Integer>}로 파싱합니다.
     * <p>4. ID 리스트가 비어 있지 않으면 {@code FieldRepository#findAllById(Iterable<Integer>)}를
     *     호출하여 해당 {@link com.gittowork.domain.field.entity.Field} 엔티티들을 조회하고,
     *     각 엔티티의 {@code getFieldName()}을 맵핑하여 이름 리스트를 생성합니다.
     * <p>5. ID 리스트와 이름 리스트를 담은 {@link GetMyInterestFieldResponse} DTO를 빌더로 만들어 반환합니다.
     *
     * @return GetMyInterestFieldResponse 사용자 관심 분야 ID 및 이름 목록
     * @throws com.gittowork.global.exception.UserNotFoundException
     *         지정된 GitHub 로그인명에 해당하는 사용자가 존재하지 않을 경우
     */
    @Transactional(readOnly = true)
    public GetMyInterestFieldResponse getInterestFields() {
        String username = authenticationFacade.getCurrentUsername();

        User user = userRepository.findByGithubName(username)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));

        List<Integer> ids = parseInterestFieldIds(user.getInterestFields());

        List<String> names = ids.isEmpty()
                ? Collections.emptyList()
                : fieldRepository.findAllById(ids).stream()
                .map(Field::getFieldName)
                .toList();

        return GetMyInterestFieldResponse.builder()
                .interestsFieldIds(ids)
                .interestsFields(names)
                .build();
    }

    /**
     * 현재 인증된 사용자의 계정을 탈퇴 처리합니다.
     *
     * <p>1. SecurityContext에서 인증된 username을 조회합니다.
     * <p>2. 해당 username으로 {@link UserRepository#findByGithubName(String)}를 통해 User 엔티티를 로드합니다.
     *     - 없으면 {@link com.gittowork.global.exception.UserNotFoundException}발생
     * <p>3. User.deleteDttm 필드에 현재 시각을 설정하고, 필요하다면 상태(state)도 DELETED로 변경합니다.
     *     (Dirty-Checking으로 커밋 시점에 UPDATE 쿼리가 자동 실행됩니다.)
     * <p>4. Redis에 저장된 “RT:{username}” 리프레시 토큰 키를 삭제합니다.
     * <p>5. 클라이언트로부터 받은 accessToken(“Bearer …”)에서 순수 토큰 문자열을 파싱해,
     *     남은 유효기간만큼 Redis 블랙리스트에 등록합니다.
     *
     * @param accessToken “Bearer ” 접두어가 제거된 순수 JWT access token
     */
    public MessageOnlyResponse deleteAccount(String accessToken) {
        String username = authenticationFacade.getCurrentUsername();

        User user = userRepository.findByGithubName(username)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));

        user.setDeleteDttm(LocalDateTime.now());

        redisService.deleteKey(username + "_refresh_token");
        if (accessToken != null && !accessToken.isEmpty()) {
            long ttl = jwtUtil.getRemainExpiredTime(accessToken);
            redisService.addTokenToBlacklist(accessToken, ttl, TimeUnit.MILLISECONDS);
        }

        return MessageOnlyResponse.builder()
                .message("회원 탈퇴 처리 완료")
                .build();
    }

    /**
     * raw 문자열(예: "[1,2,3]")을 파싱해 ID 리스트로 변환합니다.
     *
     * @param raw 관심 분야 문자열 (대괄호 포함)
     * @return 파싱된 ID 리스트. 비어 있거나 포맷이 잘못된 경우 빈 리스트 반환.
     */
    private List<Integer> parseInterestFieldIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        if (trimmed.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(trimmed.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::valueOf)
                .toList();
    }

}
