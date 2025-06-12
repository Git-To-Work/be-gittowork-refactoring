package com.gittowork.domain.interaction.service;

import com.gittowork.domain.company.entity.Company;
import com.gittowork.domain.company.repository.CompanyRepository;
import com.gittowork.domain.interaction.dto.request.InteractionGetRequest;
import com.gittowork.domain.interaction.dto.response.CompanyInteractionResponse;
import com.gittowork.domain.interaction.dto.response.Pagination;
import com.gittowork.domain.interaction.dto.response.UserInteractionResult;
import com.gittowork.domain.interaction.entity.*;
import com.gittowork.domain.interaction.repository.UserBlacklistRepository;
import com.gittowork.domain.interaction.repository.UserLikesRepository;
import com.gittowork.domain.interaction.repository.UserScrapsRepository;
import com.gittowork.domain.jobnotice.entity.JobNotice;
import com.gittowork.domain.jobnotice.repository.JobNoticeRepository;
import com.gittowork.domain.techstack.repository.NoticeTechStackRepository;
import com.gittowork.domain.user.entity.User;
import com.gittowork.domain.user.repository.UserRepository;
import com.gittowork.global.exception.CompanyNotFoundException;
import com.gittowork.global.exception.InteractionDuplicateException;
import com.gittowork.global.exception.UserInteractionNotFoundException;
import com.gittowork.global.exception.UserNotFoundException;
import com.gittowork.global.response.MessageOnlyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyInteractionService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final UserLikesRepository userLikesRepository;
    private final UserScrapsRepository userScrapsRepository;
    private final UserBlacklistRepository userBlacklistRepository;
    private final JobNoticeRepository jobNoticeRepository;
    private final NoticeTechStackRepository noticeTechStackRepository;

    private static final String ALREADY_EXISTS = "Already exists";

    /**
     * 1. 메서드 설명: 현재 인증된 사용자의 정보를 조회한다.
     * 2. 로직:
     *    - SecurityContext에서 인증 정보를 가져와 사용자를 식별한다.
     *    - 사용자 Repository를 통해 사용자를 조회한다.
     * 3. param: 없음
     * 4. return: User 객체
     */
    private User getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String githubName = authentication.getName();
        return userRepository.findByGithubName(githubName)
                .orElseThrow(() -> new UserNotFoundException(githubName));
    }

    /**
     * 1. 메서드 설명: 주어진 회사 ID에 해당하는 회사 정보를 조회한다.
     * 2. 로직:
     *    - 회사 Repository를 통해 회사 정보를 조회한다.
     * 3. param: companyId - 조회할 회사의 ID
     * 4. return: Company 객체
     */
    private Company getCompanyById(Integer companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException("Company not found"));
    }

    /**
     * 1. 메서드 설명: 상호작용 페이지 데이터를 기반으로 CompanyInteractionResponse 객체를 생성한다.
     * 2. 로직:
     *    - Page 내의 각 상호작용 엔티티에서 회사 정보를 추출한다.
     *    - Pagination 정보를 생성한다.
     *    - CompanyInteractionResponse 빌더를 통해 응답 객체를 생성한다.
     * 3. param: interactionPage - 상호작용 데이터를 포함한 Page 객체
     * 4. return: CompanyInteractionResponse 객체
     */
    private CompanyInteractionResponse buildCompanyInteractionResponse(Page<?> interactionPage) {
        int userId = getUser().getId();

        List<UserInteractionResult> results = interactionPage.stream()
                .map(interaction -> {
                    Company company;
                    // 각 상호작용 타입에 따라 Company 객체 추출
                    if (interaction instanceof UserScraps userScraps) {
                        company = userScraps.getCompany();
                    } else if (interaction instanceof UserLikes userLikes) {
                        company = userLikes.getCompany();
                    } else if (interaction instanceof UserBlacklist userBlacklist) {
                        company = userBlacklist.getCompany();
                    } else {
                        throw new IllegalArgumentException("Unsupported interaction type");
                    }

                    // 스크랩 여부를 Repository를 통해 조회
                    boolean scrapped = userScrapsRepository.existsById(new UserScrapsId(userId, company.getId()));

                    // 1. fieldName: Company의 Field 엔티티에서 필드명 추출
                    String fieldName = (company.getField() != null) ? company.getField().getFieldName() : null;

                    // 2. jobNotices 조회: 해당 회사와 연결된 JobNotice들을 조회
                    List<JobNotice> jobNotices = jobNoticeRepository.findByCompanyId(company.getId());

                    // 3. techStacks: 각 JobNotice에 연결된 NoticeTechStack에서 TechStack 이름을 수집 (중복 제거)
                    List<String> techStacks = jobNotices.stream()
                            .flatMap(notice ->
                                    noticeTechStackRepository.findByJobNoticeId(notice.getId()).stream()
                                            .map(nt -> nt.getTechStack().getTechStackName())
                            )
                            .distinct()
                            .toList();

                    // 4. hasActiveJobNotice: 현재 시각 기준 deadline_dttm이 미래인 JobNotice가 존재하면 true
                    boolean hasActiveJobNotice = jobNotices.stream()
                            .anyMatch(notice -> notice.getDeadlineDttm().isAfter(LocalDateTime.now()));

                    // UserInteractionResult DTO로 변환
                    return UserInteractionResult.builder()
                            .companyId(company.getId())
                            .companyName(company.getCompanyName())
                            .logo(company.getLogo())
                            .fieldName(fieldName)
                            .techStacks(techStacks)
                            .hasActiveJobNotice(hasActiveJobNotice)
                            .scrapped(scrapped)
                            .build();
                })
                .toList();

        Pagination pagination = new Pagination(
                interactionPage.getNumber(),
                interactionPage.getSize(),
                interactionPage.getTotalPages(),
                interactionPage.getTotalElements()
        );

        return CompanyInteractionResponse.builder()
                .companies(results)
                .pagination(pagination)
                .build();
    }


    /**
     * 1. 메서드 설명: 현재 인증된 사용자가 스크랩한 회사 목록을 조회하고, 페이징된 응답 객체를 생성한다.
     * 2. 로직:
     *    - 현재 인증된 사용자를 조회한다.
     *    - 사용자의 스크랩 데이터를 페이징 처리하여 조회한다.
     *    - 조회한 데이터를 CompanyInteractionResponse로 변환한다.
     * 3. param: interactionGetRequest - 페이지 및 사이즈 정보를 포함한 요청 객체
     * 4. return: ApiResponse 객체 (상태, 코드, 결과 및 메시지 포함)
     */
    @Transactional(readOnly = true)
    public CompanyInteractionResponse getScrapCompany(InteractionGetRequest interactionGetRequest) {
        int userId = getUser().getId();
        Pageable pageable = PageRequest.of(interactionGetRequest.getPage(), interactionGetRequest.getSize());
        Page<UserScraps> userScraps = userScrapsRepository.findByUserId(userId, pageable);

        return buildCompanyInteractionResponse(userScraps);
    }

    /**
     * 1. 메서드 설명: 현재 인증된 사용자가 스크랩한 회사 정보를 추가하고, 응답 객체를 생성한다.
     * 2. 로직:
     *    - 현재 인증된 사용자와 회사 정보를 조회한다.
     *    - 중복 체크 후 스크랩 엔티티를 생성한다.
     *    - 스크랩 엔티티를 저장하고 성공 응답을 반환한다.
     * 3. param: interactionAddRequest - 회사 ID를 포함한 요청 객체
     * 4. return: ApiResponse 객체 (상태 및 성공 메시지 포함)
     */
    @Transactional
    public MessageOnlyResponse addScrapCompany(int companyId) {
        User user = getUser();
        Company company = getCompanyById(companyId);
        UserScrapsId userScrapsId = new UserScrapsId(user.getId(), company.getId());

        if(userScrapsRepository.findById(userScrapsId).isPresent()){
            throw new InteractionDuplicateException(ALREADY_EXISTS);
        }

        UserScraps userScraps = UserScraps.builder()
                .id(userScrapsId)
                .user(user)
                .company(company)
                .build();

        userScrapsRepository.save(userScraps);
        return MessageOnlyResponse.builder().message("스크랩 추가 완료").build();
    }

    /**
     * 1. 메서드 설명: 현재 인증된 사용자의 스크랩 목록에서 지정된 회사 정보를 삭제하고, 응답 객체를 생성한다.
     * 2. 로직:
     *    - 현재 인증된 사용자와 회사 정보를 조회한다.
     *    - 해당 스크랩 엔티티가 존재하는지 확인한다.
     *    - 스크랩 엔티티를 삭제하고 성공 응답을 반환한다.
     * 3. param: interactionDeleteRequest - 삭제할 회사 ID를 포함한 요청 객체
     * 4. return: ApiResponse 객체 (상태 및 성공 메시지 포함)
     */
    @Transactional
    public MessageOnlyResponse deleteScrapCompany(int companyId) {
        User user = getUser();
        Company company = getCompanyById(companyId);
        UserScrapsId userScrapsId = new UserScrapsId(user.getId(), company.getId());

        UserScraps userScraps = userScrapsRepository.findById(userScrapsId)
                .orElseThrow(() -> new UserInteractionNotFoundException("UserScraps Not Found"));

        userScrapsRepository.delete(userScraps);
        return MessageOnlyResponse.builder().message("스크랩 삭제 요청 처리 완료").build();
    }

    /**
     * 1. 메서드 설명: 현재 인증된 사용자가 좋아요한 회사 목록을 조회하고, 페이징된 응답 객체를 생성한다.
     * 2. 로직:
     *    - 현재 인증된 사용자를 조회한다.
     *    - 사용자의 좋아요 데이터를 페이징 처리하여 조회한다.
     *    - 조회한 데이터를 CompanyInteractionResponse로 변환한다.
     * 3. param: interactionGetRequest - 페이지 및 사이즈 정보를 포함한 요청 객체
     * 4. return: ApiResponse 객체 (상태, 코드, 결과 및 메시지 포함)
     */
    @Transactional(readOnly = true)
    public CompanyInteractionResponse getMyLikeCompany(InteractionGetRequest interactionGetRequest) {
        int userId = getUser().getId();
        Pageable pageable = PageRequest.of(interactionGetRequest.getPage(), interactionGetRequest.getSize());
        Page<UserLikes> userLikes = userLikesRepository.findByUserId(userId, pageable);

        return buildCompanyInteractionResponse(userLikes);
    }

    /**
     * 1. 메서드 설명: 현재 인증된 사용자가 좋아요한 회사 정보를 추가하고, 응답 객체를 생성한다.
     * 2. 로직:
     *    - 현재 인증된 사용자와 회사 정보를 조회한다.
     *    - 중복 체크 후 좋아요 엔티티를 생성한다.
     *    - 좋아요 엔티티를 저장하고 성공 응답을 반환한다.
     * 3. param: interactionAddRequest - 회사 ID를 포함한 요청 객체
     * 4. return: ApiResponse 객체 (상태 및 성공 메시지 포함)
     */
    @Transactional
    public MessageOnlyResponse addLikeCompany(int companyId) {
        User user = getUser();
        Company company = getCompanyById(companyId);
        UserLikesId userLikesId = new UserLikesId(user.getId(), company.getId());

        if (userLikesRepository.findById(userLikesId).isPresent()) {
            throw new InteractionDuplicateException(ALREADY_EXISTS);
        }

        UserLikes userLikes = UserLikes.builder()
                .id(userLikesId)
                .user(user)
                .company(company)
                .build();

        userLikesRepository.save(userLikes);
        return MessageOnlyResponse.builder().message("좋아요 요청 처리 완료").build();
    }

    /**
     * 1. 메서드 설명: 현재 인증된 사용자의 좋아요 목록에서 지정된 회사 정보를 삭제하고, 응답 객체를 생성한다.
     * 2. 로직:
     *    - 현재 인증된 사용자와 회사 정보를 조회한다.
     *    - 해당 좋아요 엔티티가 존재하는지 확인한다.
     *    - 좋아요 엔티티를 삭제하고 성공 응답을 반환한다.
     * 3. param: interactionDeleteRequest - 삭제할 회사 ID를 포함한 요청 객체
     * 4. return: ApiResponse 객체 (상태 및 성공 메시지 포함)
     */
    @Transactional
    public MessageOnlyResponse deleteLikeCompany(int companyId) {
        User user = getUser();
        Company company = getCompanyById(companyId);
        UserLikesId userLikesId = new UserLikesId(user.getId(), company.getId());

        UserLikes userLikes = userLikesRepository.findById(userLikesId)
                .orElseThrow(() -> new UserInteractionNotFoundException("UserLikes Not Found"));

        userLikesRepository.delete(userLikes);
        return MessageOnlyResponse.builder().message("좋아요 삭제 요청 처리 완료").build();
    }

    /**
     * 1. 메서드 설명: 현재 인증된 사용자가 블랙리스트에 등록한 회사 목록을 조회하고, 페이징된 응답 객체를 생성한다.
     * 2. 로직:
     *    - 현재 인증된 사용자를 조회한다.
     *    - 사용자의 블랙리스트 데이터를 페이징 처리하여 조회한다.
     *    - 조회한 데이터를 CompanyInteractionResponse로 변환한다.
     * 3. param: interactionGetRequest - 페이지 및 사이즈 정보를 포함한 요청 객체
     * 4. return: ApiResponse 객체 (상태, 코드, 결과 및 메시지 포함)
     */
    @Transactional(readOnly = true)
    public CompanyInteractionResponse getMyBlackList(InteractionGetRequest interactionGetRequest) {
        int userId = getUser().getId();
        Pageable pageable = PageRequest.of(interactionGetRequest.getPage(), interactionGetRequest.getSize());
        Page<UserBlacklist> userBlacklist = userBlacklistRepository.findByUserId(userId, pageable);

        return buildCompanyInteractionResponse(userBlacklist);
    }

    /**
     * 1. 메서드 설명: 현재 인증된 사용자가 블랙리스트에 등록한 회사 정보를 추가하고, 응답 객체를 생성한다.
     * 2. 로직:
     *    - 현재 인증된 사용자와 회사 정보를 조회한다.
     *    - 중복 체크 후 블랙리스트 엔티티를 생성한다.
     *    - 블랙리스트 엔티티를 저장하고 성공 응답을 반환한다.
     * 3. param: interactionAddRequest - 회사 ID를 포함한 요청 객체
     * 4. return: ApiResponse 객체 (상태 및 성공 메시지 포함)
     */
    @Transactional
    public MessageOnlyResponse addMyBlackList(int companyId) {
        User user = getUser();
        Company company = getCompanyById(companyId);
        UserBlacklistId userBlacklistId = new UserBlacklistId(user.getId(), company.getId());

        if (userBlacklistRepository.findById(userBlacklistId).isPresent()) {
            throw new InteractionDuplicateException(ALREADY_EXISTS);
        }

        UserBlacklist userBlacklist = UserBlacklist.builder()
                .id(userBlacklistId)
                .user(user)
                .company(company)
                .build();

        userBlacklistRepository.save(userBlacklist);
        userScrapsRepository.deleteById(new UserScrapsId(user.getId(), company.getId()));
        userLikesRepository.deleteById(new UserLikesId(user.getId(), company.getId()));

        return MessageOnlyResponse.builder().message("차단 기업 추가 완료").build();
    }

    /**
     * 1. 메서드 설명: 현재 인증된 사용자의 블랙리스트 목록에서 지정된 회사 정보를 삭제하고, 응답 객체를 생성한다.
     * 2. 로직:
     *    - 현재 인증된 사용자와 회사 정보를 조회한다.
     *    - 해당 블랙리스트 엔티티가 존재하는지 확인한다.
     *    - 블랙리스트 엔티티를 삭제하고 성공 응답을 반환한다.
     * 3. param: interactionDeleteRequest - 삭제할 회사 ID를 포함한 요청 객체
     * 4. return: ApiResponse 객체 (상태 및 성공 메시지 포함)
     */
    @Transactional
    public MessageOnlyResponse deleteMyBlackList(int companyId) {
        User user = getUser();
        Company company = getCompanyById(companyId);
        UserBlacklistId userBlacklistId = new UserBlacklistId(user.getId(), company.getId());

        UserBlacklist userBlacklist = userBlacklistRepository.findById(userBlacklistId)
                .orElseThrow(() -> new UserInteractionNotFoundException("UserBlacklist Not Found"));

        userBlacklistRepository.delete(userBlacklist);
        return MessageOnlyResponse.builder().message("차단 기업 삭제 요청 처리 완료").build();
    }
}
