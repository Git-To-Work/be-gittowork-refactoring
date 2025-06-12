package com.gittowork.domain.user.service;

import com.gittowork.domain.field.entity.Field;
import com.gittowork.domain.field.repository.FieldRepository;
import com.gittowork.domain.github.service.GithubAnalysisService;
import com.gittowork.domain.user.dto.request.InsertProfileRequest;
import com.gittowork.domain.user.dto.request.UpdateInterestsFieldsRequest;
import com.gittowork.domain.user.dto.request.UpdateProfileRequest;
import com.gittowork.domain.user.dto.response.GetInterestFieldsResponse;
import com.gittowork.domain.user.dto.response.GetMyInterestFieldResponse;
import com.gittowork.domain.user.dto.response.GetMyProfileResponse;

import com.gittowork.domain.user.entity.User;
import com.gittowork.domain.user.entity.UserGitInfo;
import com.gittowork.domain.user.repository.UserGitInfoRepository;
import com.gittowork.domain.user.repository.UserRepository;
import com.gittowork.global.exception.DataNotFoundException;
import com.gittowork.global.exception.UserNotFoundException;
import com.gittowork.global.response.MessageOnlyResponse;
import com.gittowork.global.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserGitInfoRepository userGitInfoRepository;
    private final RedisService redisService;
    private final FieldRepository fieldRepository;
    private final GithubAnalysisService githubAnalysisService;

    private static final String USER_NOT_FOUND = "User not found";

    /**
     * 1. 메서드 설명: 프로필 추가 정보를 저장하는 API.
     * 2. 로직:
     *    - 현재 인증 정보에서 username을 조회하고, Redis에서 사용자 기본 정보와 GitHub 추가 정보를 가져온다.
     *    - 조회한 데이터를 바탕으로 User 엔티티를 생성 및 저장하여 auto increment된 id를 확보한다.
     *    - 해당 id를 기반으로 UserGitInfo 엔티티를 생성하고, User와 양방향 연관관계를 설정한 후 저장한다.
     *    - User의 Githhub Repository 정보를 저장하는 비동기 메서드를 실행한다.
     * 3. param: insertProfileRequest - 프로필 추가 정보를 담은 DTO.
     * 4. return: 성공 시 "추가 정보가 성공적으로 업데이트되었습니다." 메시지를 포함한 MessageOnlyResponse 객체.
     */
    @Transactional
    public MessageOnlyResponse insertProfile(InsertProfileRequest insertProfileRequest) {
        String username = getUserName();

        Map<Object, Object> userCaching = redisService.getUser("user: " + username);

        if (userCaching == null || userCaching.isEmpty()) {
            throw new DataNotFoundException("캐싱된 사용자 기본 정보가 없습니다.");
        }

        Integer githubId = (Integer) userCaching.get("githubId");

        User user = User.builder()
                .githubId(githubId)
                .githubName(userCaching.get("githubName").toString())
                .name(insertProfileRequest.getName())
                .githubEmail(userCaching.get("githubEmail") == null ? null : userCaching.get("githubEmail").toString())
                .phone(insertProfileRequest.getPhone())
                .birthDt(LocalDate.parse(insertProfileRequest.getBirthDt()))
                .experience(insertProfileRequest.getExperience())
                .createDttm(LocalDateTime.now())
                .updateDttm(LocalDateTime.now())
                .privacyConsentDttm(insertProfileRequest.isPrivacyPolicyAgreed() ? LocalDateTime.now() : null)
                .githubAccessToken(userCaching.get("githubAccessToken").toString())
                .notificationAgreeDttm(insertProfileRequest.isNotificationAgreed() ? LocalDateTime.now() : null)
                .build();

        user = userRepository.save(user);

        log.info(user.toString());

        Map<Object, Object> userGitInfoCaching = redisService.getUserGitInfo("userGitInfo: " + username);

        if (userGitInfoCaching == null || userGitInfoCaching.isEmpty()) {
            throw new DataNotFoundException("캐싱된 깃허브 사용자 기본 정보가 없습니다.");
        }

        UserGitInfo userGitInfo = UserGitInfo.builder()
                .avatarUrl(userGitInfoCaching.get("githubAvatarUrl").toString())
                .publicRepositories((Integer) userGitInfoCaching.get("publicRepositories"))
                .followers((Integer) userGitInfoCaching.get("followers"))
                .followings((Integer) userGitInfoCaching.get("following"))
                .createDttm(LocalDateTime.now())
                .updateDttm(LocalDateTime.now())
                .build();

        userGitInfo.setUser(user);
        user.setUserGitInfo(userGitInfo);

        userGitInfoRepository.save(userGitInfo);

        redisService.deleteKey("user:" + username);
        redisService.deleteKey("userGitInfo:" + username);

        githubAnalysisService.saveUserGithubRepositoryInfo(user.getGithubAccessToken(), username, user.getId());

        return MessageOnlyResponse.builder()
                .message("추가 정보가 성공적으로 업데이트되었습니다.")
                .build();
    }

    /**
     * 1. 메서드 설명: 내 프로필 정보를 조회하는 API.
     * 2. 로직:
     *    - 현재 인증 정보에서 username을 조회하고, username으로 DB에서 User 엔티티를 찾는다. (없으면 예외 발생)
     *    - User의 interestFields 문자열에서 대괄호를 제거하고, 쉼표 기준으로 분리하여 관심 분야 ID 리스트를 생성한다.
     *    - 생성된 ID 리스트로 Fields 엔티티들을 조회하고, fieldName만 추출하여 String 배열로 변환한다.
     *    - User 정보와 GitHub 프로필 정보를 바탕으로 GetMyProfileResponse를 생성해 반환한다.
     * 3. param: 없음.
     * 4. return: 내 프로필 정보를 담은 GetMyProfileResponse 객체.
     */
    @Transactional(readOnly = true)
    public GetMyProfileResponse getMyProfile() {
        String username = getUserName();

        User user = userRepository.findByGithubName(username)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return GetMyProfileResponse.builder()
                .userId(user.getId())
                .email(user.getGithubEmail())
                .name(user.getName())
                .nickname(user.getGithubName())
                .phone(user.getPhone())
                .birthDt(user.getBirthDt().format(formatter))
                .experience(user.getExperience())
                .avatarUrl(user.getUserGitInfo().getAvatarUrl())
                .notificationAgreed(user.getNotificationAgreeDttm() != null)
                .build();
    }

    /**
     * 1. 메서드 설명: 프로필 추가 정보를 수정하는 API.
     * 2. 로직:
     *    - 전달받은 updateProfileRequest의 사용자 id를 사용해 DB에서 User 엔티티를 조회한다. (없으면 예외 발생)
     *    - 조회한 User 엔티티의 이름, 생년월일, 경력, 전화번호, 관심 분야 정보를 updateProfileRequest의 값으로 업데이트한다.
     *    - 업데이트된 User 엔티티를 저장하고, 성공 메시지를 포함한 MessageOnlyResponse를 반환한다.
     * 3. param: updateProfileRequest - 프로필 수정 정보를 담은 DTO.
     * 4. return: 성공 시 "추가 정보 수정 요청 처리 완료" 메시지를 포함한 MessageOnlyResponse 객체.
     */
    @Transactional
    public MessageOnlyResponse updateProfile(UpdateProfileRequest updateProfileRequest) {
        User user = userRepository.findById(updateProfileRequest.getUserId())
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));

        user.setName(updateProfileRequest.getName());
        user.setBirthDt(LocalDate.parse(updateProfileRequest.getBirthDt()));
        user.setExperience(updateProfileRequest.getExperience());
        user.setPhone(updateProfileRequest.getPhone());

        if (user.getNotificationAgreeDttm() != null) {
            if (!updateProfileRequest.isNotificationAgreed()) {
                user.setNotificationAgreeDttm(null);
            }
        } else {
            if (updateProfileRequest.isNotificationAgreed()) {
                user.setNotificationAgreeDttm(LocalDateTime.now());
            }
        }

        userRepository.save(user);

        return MessageOnlyResponse.builder()
                .message("추가 정보 수정 요청 처리 완료")
                .build();
    }

    /**
     * 1. 메서드 설명: 모든 관심 분야(Fields) 목록을 조회하여 GetInterestFieldsResponse 객체로 반환하는 API.
     * 2. 로직:
     *    - FieldsRepository의 findAll()을 통해 DB에서 모든 Fields 엔티티를 조회한다.
     *    - 조회한 결과를 GetInterestFieldsResponse 빌더를 사용해 Response 객체로 변환하여 반환한다.
     * 3. param: 없음.
     * 4. return: 모든 관심 분야 목록을 포함하는 GetInterestFieldsResponse 객체.
     */
    @Transactional(readOnly = true)
    public GetInterestFieldsResponse getInterestFields() {
        List<Field> interestFields = fieldRepository.findAll();

        return GetInterestFieldsResponse.builder()
                .fields(interestFields)
                .build();
    }

    /**
     * 1. 메서드 설명: 현재 인증된 사용자의 관심 비즈니스 분야 정보를 업데이트하는 API.
     * 2. 로직:
     *    - 현재 인증 정보에서 username을 조회하고, 해당 사용자를 DB에서 조회한다. (없으면 예외 발생)
     *    - 전달받은 UpdateInterestsFieldsRequest의 interestsFields 배열을 문자열로 변환 후 공백을 제거하여 User 엔티티의 관심 분야 정보에 설정한다.
     *    - 변경된 User 엔티티를 저장하고, 성공 메시지를 포함한 MessageOnlyResponse를 반환한다.
     * 3. param: updateInterestsFieldsRequest - 관심 분야 정보를 담은 DTO.
     * 4. return: 성공 메시지를 포함한 MessageOnlyResponse 객체.
     */
    @Transactional
    public MessageOnlyResponse updateInterestFields(UpdateInterestsFieldsRequest updateInterestsFieldsRequest) {
        String username = getUserName();

        User user = userRepository.findByGithubName(username)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));

        String interestFields = Arrays.toString(updateInterestsFieldsRequest.getInterestsFields()).replaceAll(" ", "");
        user.setInterestFields(interestFields);

        userRepository.save(user);

        return MessageOnlyResponse.builder()
                .message("관심 비즈니스 분야 수정 처리 성공")
                .build();
    }

    /**
     * 1. 메서드 설명: 현재 인증된 사용자의 관심 비즈니스 분야 정보를 조회하여 응답 객체로 반환하는 API.
     * 2. 로직:
     *    - 현재 인증 정보에서 username을 조회하고, username으로 DB에서 User 엔티티를 찾는다. (없으면 예외 발생)
     *    - User의 interestFields 문자열에서 대괄호를 제거하고 쉼표를 기준으로 분리하여 관심 분야 ID 리스트를 생성한다.
     *    - 생성된 ID 리스트로 Fields 엔티티들을 조회하고, 각 엔티티의 fieldName만 추출하여 String 배열로 변환한다.
     *    - GetMyInterestFieldResponse 빌더를 사용해 응답 객체를 생성 후 반환한다.
     * 3. param: 없음.
     * 4. return: 관심 비즈니스 분야 이름을 포함하는 GetMyInterestFieldResponse 객체.
     */
    @Transactional(readOnly = true)
    public GetMyInterestFieldResponse myInterestFields() {
        String username = getUserName();

        String[] fieldsNames = resolveInterestFieldNames(username);
        int[] fieldsIds = resolveInterestFieldIds(username);

        return GetMyInterestFieldResponse.builder()
                .interestsFields(fieldsNames)
                .interestsFieldIds(fieldsIds)
                .build();
    }

    /**
     * 1. 메서드 설명: 현재 인증된 사용자의 회원 탈퇴를 처리하는 API.
     * 2. 로직:
     *    - Spring Security의 SecurityContextHolder에서 현재 인증된 사용자(username)를 조회한다.
     *    - username을 이용해 DB에서 해당 User 엔티티를 조회한다. (없으면 예외 발생)
     *    - User 엔티티의 deleteDttm 필드에 현재 시간을 기록하여 논리적 삭제를 수행하고 저장한다.
     *    - Redis에서 username+"_refresh_token" 키를 삭제하여 refresh token을 제거한다.
     *    - Authentication 객체의 credentials에서 access token 값을 추출하고, "Bearer " 접두어가 포함된 경우 제거한 후 해당 access token을 블랙리스트에 등록한다.
     * 3. param: 없음.
     * 4. return: 회원 탈퇴 처리 결과 메시지를 포함하는 MessageOnlyResponse 객체.
     */
    @Transactional
    public MessageOnlyResponse deleteAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByGithubName(username)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));

        user.setDeleteDttm(LocalDateTime.now());
        userRepository.save(user);

        redisService.deleteKey(username + "_refresh_token");

        String accessToken = null;
        if (authentication.getCredentials() instanceof String string) {
            accessToken = string;
            if (accessToken.startsWith("Bearer ")) {
                accessToken = accessToken.substring(7);
            }
        }
        if (accessToken != null && !accessToken.isEmpty()) {
            redisService.addAccessTokenToBlacklist(accessToken);
        }

        return MessageOnlyResponse.builder()
                .message("회원 탈퇴 처리 완료")
                .build();
    }

    /**
     * 1. 메서드 설명: 현재 SecurityContextHolder에서 username을 추출하는 헬퍼 메서드.
     * 2. 로직:
     *    - SecurityContextHolder에서 현재 인증 정보를 조회하여 username을 반환한다.
     * 3. param: 없음.
     * 4. return: 현재 인증된 사용자의 username 문자열.
     */
    private String getUserName() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /**
     * 1. 메서드 설명: 주어진 username에 해당하는 User 엔티티의 interestFields 값을 기반으로 관심 분야 이름들을 조회하여 String 배열로 반환하는 헬퍼 메서드.
     * 2. 로직:
     *    - username을 이용해 DB에서 User 엔티티를 조회한다. (없으면 예외 발생)
     *    - User의 interestFields 문자열에서 대괄호를 제거하고 쉼표를 기준으로 분리하여 관심 분야 ID 리스트를 생성한다.
     *    - 생성된 ID 리스트로 Fields 엔티티들을 조회하고, 각 엔티티의 fieldName만 추출하여 String 배열로 변환한다.
     * 3. param: username - 관심 분야 정보를 조회할 대상 사용자의 username.
     * 4. return: 해당 사용자의 관심 분야 이름들을 담은 String 배열.
     */
    private String[] resolveInterestFieldNames(String username) {
        User user = userRepository.findByGithubName(username)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));

        List<Integer> interestFieldsNumbers = Arrays.stream(
                        user.getInterestFields().replaceAll("[\\[\\]]", "").split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        List<Field> interestFields = fieldRepository.findAllById(interestFieldsNumbers);

        return interestFields.stream()
                .map(Field::getFieldName)
                .toArray(String[]::new);
    }

    private int[] resolveInterestFieldIds(String username) {
        User user = userRepository.findByGithubName(username)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));

        return Arrays.stream(user.getInterestFields().replaceAll("[\\[\\]]", "").split(","))
                .map(String::trim)
                .mapToInt(Integer::parseInt)
                .toArray();
    }
}
