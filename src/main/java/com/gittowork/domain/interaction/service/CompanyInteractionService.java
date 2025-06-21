package com.gittowork.domain.interaction.service;

import com.gittowork.domain.company.entity.Company;
import com.gittowork.domain.company.repository.CompanyRepository;
import com.gittowork.domain.interaction.dto.response.CompanyInteractionResponse;
import com.gittowork.domain.interaction.dto.response.UserInteractionResult;
import com.gittowork.domain.interaction.entity.*;
import com.gittowork.domain.interaction.repository.UserBlacklistRepository;
import com.gittowork.domain.interaction.repository.UserLikesRepository;
import com.gittowork.domain.interaction.repository.UserScrapsRepository;
import com.gittowork.domain.jobnotice.entity.JobNotice;
import com.gittowork.domain.jobnotice.repository.JobNoticeRepository;
import com.gittowork.domain.techstack.entity.NoticeTechStack;
import com.gittowork.domain.techstack.repository.NoticeTechStackRepository;
import com.gittowork.domain.user.entity.User;
import com.gittowork.domain.user.repository.UserRepository;
import com.gittowork.global.exception.company.CompanyNotFoundException;
import com.gittowork.global.exception.interaction.InteractionDuplicateException;
import com.gittowork.global.exception.interaction.UserInteractionNotFoundException;
import com.gittowork.global.exception.auth.UserNotFoundException;
import com.gittowork.global.facade.AuthenticationFacade;
import com.gittowork.global.dto.response.MessageOnlyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * 사용자와 회사 간 상호작용(스크랩, 좋아요, 블랙리스트) 비즈니스 로직을 처리하는 서비스 클래스입니다.
 */
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
    private final AuthenticationFacade authenticationFacade;

    private static final String ALREADY_EXISTS = "Already exists";

    /**
     * 현재 인증된 사용자가 스크랩한 회사 목록을 페이지 단위로 조회합니다.
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 페이징된 회사 상호작용 응답 DTO
     */
    @Transactional(readOnly = true)
    public CompanyInteractionResponse getScrapCompany(int page, int size) {
        User user = loadCurrentUser();
        Page<UserScraps> scrapsPage = userScrapsRepository.findByUserId(user.getId(), PageRequest.of(page, size));
        return processInteractions(scrapsPage, user.getId());
    }

    /**
     * 현재 인증된 사용자가 특정 회사를 스크랩 목록에 추가합니다.
     *
     * @param companyId 스크랩할 회사 ID
     * @return 처리 결과 메시지
     */
    @Transactional
    public MessageOnlyResponse addScrapCompany(Integer companyId) {
        User user = loadCurrentUser();
        Company company = getCompanyById(companyId);
        UserScrapsId userScrapsId = new UserScrapsId(user.getId(), company.getId());

        if (userScrapsRepository.findById(userScrapsId).isPresent()) {
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
     * 현재 인증된 사용자의 스크랩 목록에서 특정 회사를 삭제합니다.
     *
     * @param companyId 삭제할 회사 ID
     * @return 처리 결과 메시지
     */
    @Transactional
    public MessageOnlyResponse deleteScrapCompany(int companyId) {
        User user = loadCurrentUser();
        Company company = getCompanyById(companyId);
        UserScrapsId userScrapsId = new UserScrapsId(user.getId(), company.getId());

        UserScraps userScraps = userScrapsRepository.findById(userScrapsId)
                .orElseThrow(() -> new UserInteractionNotFoundException("UserScraps Not Found"));

        userScrapsRepository.delete(userScraps);
        return MessageOnlyResponse.builder().message("스크랩 삭제 요청 처리 완료").build();
    }

    /**
     * 현재 인증된 사용자가 좋아요한 회사 목록을 페이지 단위로 조회합니다.
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 페이징된 회사 상호작용 응답 DTO
     */
    @Transactional(readOnly = true)
    public CompanyInteractionResponse getMyLikeCompany(int page, int size) {
        User user = loadCurrentUser();
        Page<UserLikes> likesPage = userLikesRepository.findByUserId(user.getId(), PageRequest.of(page, size));
        return processInteractions(likesPage, user.getId());
    }

    /**
     * 현재 인증된 사용자가 특정 회사를 좋아요 목록에 추가합니다.
     *
     * @param companyId 좋아요할 회사 ID
     * @return 처리 결과 메시지
     */
    @Transactional
    public MessageOnlyResponse addLikeCompany(int companyId) {
        User user = loadCurrentUser();
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
     * 현재 인증된 사용자의 좋아요 목록에서 특정 회사를 삭제합니다.
     *
     * @param companyId 삭제할 회사 ID
     * @return 처리 결과 메시지
     */
    @Transactional
    public MessageOnlyResponse deleteLikeCompany(int companyId) {
        User user = loadCurrentUser();
        Company company = getCompanyById(companyId);
        UserLikesId userLikesId = new UserLikesId(user.getId(), company.getId());

        UserLikes userLikes = userLikesRepository.findById(userLikesId)
                .orElseThrow(() -> new UserInteractionNotFoundException("UserLikes Not Found"));

        userLikesRepository.delete(userLikes);
        return MessageOnlyResponse.builder().message("좋아요 삭제 요청 처리 완료").build();
    }

    /**
     * 현재 인증된 사용자가 블랙리스트에 등록한 회사 목록을 페이지 단위로 조회합니다.
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 페이징된 회사 상호작용 응답 DTO
     */
    @Transactional(readOnly = true)
    public CompanyInteractionResponse getMyBlackList(int page, int size) {
        User user = loadCurrentUser();
        Page<UserBlacklist> blacklistPage = userBlacklistRepository.findByUserId(user.getId(), PageRequest.of(page, size));
        return processInteractions(blacklistPage, user.getId());
    }

    /**
     * 현재 인증된 사용자가 특정 회사를 블랙리스트에 추가합니다.
     *
     * @param companyId 블랙리스트에 추가할 회사 ID
     * @return 처리 결과 메시지
     */
    @Transactional
    public MessageOnlyResponse addMyBlackList(int companyId) {
        User user = loadCurrentUser();
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
     * 현재 인증된 사용자의 블랙리스트에서 특정 회사를 삭제합니다.
     *
     * @param companyId 삭제할 회사 ID
     * @return 처리 결과 메시지
     */
    @Transactional
    public MessageOnlyResponse deleteMyBlackList(int companyId) {
        User user = loadCurrentUser();
        Company company = getCompanyById(companyId);
        UserBlacklistId userBlacklistId = new UserBlacklistId(user.getId(), company.getId());

        UserBlacklist userBlacklist = userBlacklistRepository.findById(userBlacklistId)
                .orElseThrow(() -> new UserInteractionNotFoundException("UserBlacklist Not Found"));

        userBlacklistRepository.delete(userBlacklist);
        return MessageOnlyResponse.builder().message("차단 기업 삭제 요청 처리 완료").build();
    }

    /**
     * interactionPage와 userId를 바탕으로 공통 결과 생성 로직을 수행합니다.
     *
     * @param interactionPage 스크랩/좋아요/블랙리스트 페이지
     * @param userId 사용자 ID
     * @return 회사 상호작용 응답 DTO
     */
    private CompanyInteractionResponse processInteractions(Page<?> interactionPage, int userId) {
        List<Integer> companyIds = interactionPage.stream()
                .map(this::getCompany)
                .map(Company::getId)
                .toList();

        List<JobNotice> allNotices = jobNoticeRepository.findByCompanyIdIn(companyIds);
        Map<Integer, List<JobNotice>> noticesByCompany = allNotices.stream()
                .collect(Collectors.groupingBy(n -> n.getCompany().getId()));

        List<Integer> noticeIds = allNotices.stream().map(JobNotice::getId).toList();
        List<NoticeTechStack> allStacks = noticeTechStackRepository.findByJobNoticeIdIn(noticeIds);
        Map<Integer, List<String>> stacksByNotice = allStacks.stream()
                .collect(Collectors.groupingBy(
                        nt -> nt.getJobNotice().getId(),
                        Collectors.mapping(nt -> nt.getTechStack().getTechStackName(), toList())
                ));

        Set<Integer> scrapedSet = new HashSet<>(
                userScrapsRepository.findCompanyIdsByUserId(userId, companyIds)
        );

        List<UserInteractionResult> results = interactionPage.stream().map(inter -> {
            Company c = getCompany(inter);
            List<JobNotice> compNotices = noticesByCompany.getOrDefault(c.getId(), List.of());
            boolean hasActive = compNotices.stream()
                    .anyMatch(n -> n.getDeadlineDttm().isAfter(LocalDateTime.now()));

            List<String> techs = compNotices.stream()
                    .flatMap(n -> stacksByNotice.getOrDefault(n.getId(), List.of()).stream())
                    .distinct()
                    .toList();

            return UserInteractionResult.builder()
                    .companyId(c.getId())
                    .companyName(c.getCompanyName())
                    .logo(c.getLogo())
                    .fieldName(c.getField() != null ? c.getField().getFieldName() : null)
                    .techStacks(techs)
                    .hasActiveJobNotice(hasActive)
                    .scrapped(scrapedSet.contains(c.getId()))
                    .build();
        }).toList();

        return CompanyInteractionResponse.builder()
                .companies(results)
                .currentPage(interactionPage.getNumber())
                .pageSize(interactionPage.getSize())
                .totalPages(interactionPage.getTotalPages())
                .totalItems(interactionPage.getTotalElements())
                .build();
    }

    /**
     * interaction 타입에 따라 Company 객체를 반환합니다.
     *
     * @param interaction UserScraps, UserLikes, UserBlacklist 중 하나
     * @return 상호작용된 Company
     */
    private Company getCompany(Object interaction) {
        if (interaction instanceof UserScraps us) return us.getCompany();
        if (interaction instanceof UserLikes ul) return ul.getCompany();
        if (interaction instanceof UserBlacklist ub) return ub.getCompany();
        throw new IllegalArgumentException("Unsupported interaction type");
    }

    /**
     * 현재 인증된 사용자를 조회합니다.
     *
     * @return User 엔티티
     */
    private User loadCurrentUser() {
        String username = authenticationFacade.getCurrentUsername();
        return userRepository.findByGithubName(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    /**
     * 회사 ID로 Company 엔티티를 조회합니다.
     *
     * @param companyId 회사 ID
     * @return Company 엔티티
     */
    private Company getCompanyById(Integer companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException("Company not found"));
    }
}
