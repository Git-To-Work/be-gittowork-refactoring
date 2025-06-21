package com.gittowork.domain.github.service;

import com.gittowork.domain.github.dto.response.*;
import com.gittowork.domain.github.entity.*;
import com.gittowork.domain.github.model.analysis.RepositoryCombination;
import com.gittowork.domain.github.model.repository.Repo;
import com.gittowork.domain.github.model.repository.Repository;
import com.gittowork.domain.github.repository.*;
import com.gittowork.domain.user.entity.User;
import com.gittowork.domain.user.repository.UserRepository;
import com.gittowork.global.exception.auth.UserNotFoundException;
import com.gittowork.global.exception.github.GithubAnalysisNotFoundException;
import com.gittowork.global.exception.github.GithubRepositoryNotFoundException;
import com.gittowork.global.exception.github.SelectedRepositoryDuplicatedException;
import com.gittowork.global.facade.AuthenticationFacade;
import com.gittowork.global.dto.response.MessageOnlyResponse;
import com.gittowork.global.service.github.GithubRestApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GitHub 연동 및 분석 관련 주요 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * <p>
 * - 사용자의 저장된 Repository 조회
 * - 선택된 Repository 조합 저장, 조회, 삭제
 * - GitHub 이벤트 체크 및 분석 요청
 * - 분석 상태 조회 및 결과 반환
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GithubService {

    private final GithubAnalysisResultRepository githubAnalysisResultRepository;
    private final GithubRepoRepository githubRepoRepository;
    private final UserRepository userRepository;
    private final SelectedRepoRepository selectedRepoRepository;
    private final GithubRestApiService githubRestApiService;
    private final GithubAnalysisService githubAnalysisService;
    private final AnalysisStatusRepository analysisStatusRepository;
    private final AuthenticationFacade authenticationFacade;

    private static final String USER_NOT_FOUND = "User not found";

    /**
     * 지정된 조합 ID에 대한 분석 결과를 조회합니다.
     * <p>
     * 분석 상태가 COMPLETE인 경우 결과 상세 정보를, 그렇지 않은 경우 현재 상태 메시지를 반환합니다.
     * </p>
     *
     * @param selectedRepositoryId 분석 조합의 고유 ID
     * @return 분석 상태 및 결과를 담은 {@link GithubAnalysisResponse}
     * @throws GithubAnalysisNotFoundException 해당 분석 상태가 없을 경우 발생
     */
    @Transactional(readOnly = true)
    public GithubAnalysisResponse getGithubAnalysisByRepository(String selectedRepositoryId) {
        AnalysisStatus status = analysisStatusRepository
                .findBySelectedRepositoriesId(selectedRepositoryId)
                .orElseThrow(() -> new GithubAnalysisNotFoundException("Github analysis status not found"));

        if (status.getStatus() == AnalysisStatus.Status.COMPLETE) {
            return buildCompleteResponse(selectedRepositoryId, status);
        }
        return buildNotCompleteResponse(selectedRepositoryId, status);
    }

    /**
     * 사용자가 선택한 리포지토리 ID 배열로 신규 분석을 생성하거나,
     * 기존 분석 조합을 재사용하여 분석을 시작합니다.
     *
     * @param repoIds 선택된 GitHub 리포지토리 ID 배열
     * @return 분석 시작 결과 및 조합 정보를 담은 {@link CreateGithubAnalysisByRepositoryResponse}
     * @throws UserNotFoundException 사용자 정보를 찾을 수 없을 때 발생
     * @throws GithubRepositoryNotFoundException 사용자의 GitHub 리포지토리 정보가 없을 때 발생
     */
    @Transactional
    public CreateGithubAnalysisByRepositoryResponse createGithubAnalysisByRepositoryResponse(int[] repoIds) {
        String userName = authenticationFacade.getCurrentUsername();
        User user = userRepository.findByGithubName(userName)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));
        int userId = user.getId();
        String token = user.getGithubAccessToken();

        Set<Integer> idSet = Arrays.stream(repoIds).boxed().collect(Collectors.toSet());
        GithubRepository repoDoc = githubRepoRepository.findByUserId(userId)
                .orElseThrow(() -> new GithubRepositoryNotFoundException("Github Repository not found"));
        List<Repository> allRepos = repoDoc.getRepositories();
        List<String> names = allRepos.stream()
                .filter(r -> idSet.contains(r.getRepoId()))
                .map(Repository::getRepoName)
                .collect(Collectors.toList());

        boolean started = githubRestApiService.checkNewGithubEvents(token, userName, userId, names);
        String comboId = null;
        if (started) {
            githubAnalysisService.saveUserGithubRepositoryInfo(token, userName, userId);
            githubAnalysisService.githubAnalysisByRepository(repoIds, userName);

            SelectedRepository sel = selectedRepoRepository
                    .findByUserIdAndRepositories(userId,
                            allRepos.stream()
                                    .filter(r -> idSet.contains(r.getRepoId()))
                                    .collect(Collectors.toList()))
                    .orElseThrow(() -> new GithubRepositoryNotFoundException("Combination not found"));
            comboId = sel.getSelectedRepositoryId();

            analysisStatusRepository.updateStatusByUserIdAndSelectedRepositoriesId(
                    userId, comboId, AnalysisStatus.Status.ANALYZING);
        }
        return CreateGithubAnalysisByRepositoryResponse.builder()
                .analysisStarted(started)
                .selectedRepositoryId(comboId)
                .selectedRepositories(names)
                .message(started ? "분석이 시작되었습니다." : "추가 이벤트가 없습니다.")
                .build();
    }

    /**
     * 사용자가 선택한 리포지토리 조합을 저장합니다.
     * <p>
     * 이미 동일한 조합이 존재하면 예외를 발생시킵니다.
     * </p>
     *
     * @param selectedGithubRepositoryIds 선택된 리포지토리 ID 배열
     * @return 저장된 조합 ID 및 처리 메시지를 담은 {@link SaveSelectedRepositoriesResponse}
     * @throws UserNotFoundException 인증된 사용자 정보가 없을 때 발생
     * @throws SelectedRepositoryDuplicatedException 동일 조합이 이미 존재할 때 발생
     */
    @Transactional
    public SaveSelectedRepositoriesResponse saveSelectedGithubRepository(int[] selectedGithubRepositoryIds) {
        String userName = authenticationFacade.getCurrentUsername();
        User user = userRepository.findByGithubName(userName)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));
        int userId = user.getId();

        GithubRepository repoDoc = githubRepoRepository.findByUserId(userId)
                .orElseThrow(() -> new GithubRepositoryNotFoundException("Github repository not found"));
        List<Repository> allRepos = repoDoc.getRepositories();
        Set<Integer> idSet = Arrays.stream(selectedGithubRepositoryIds).boxed().collect(Collectors.toSet());
        List<Repository> selRepos = new ArrayList<>();
        for (Repository r : allRepos) {
            if (idSet.contains(r.getRepoId())) selRepos.add(r);
        }

        boolean exists = selectedRepoRepository.existsByUserIdAndRepositoriesIn(userId, selRepos);
        if (exists) {
            throw new SelectedRepositoryDuplicatedException("Selected repository already exists");
        }
        SelectedRepository newSel = SelectedRepository.builder()
                .userId(userId)
                .repositories(selRepos)
                .build();
        newSel = selectedRepoRepository.save(newSel);

        analysisStatusRepository.save(AnalysisStatus.builder()
                .user(user)
                .selectedRepositoriesId(newSel.getSelectedRepositoryId())
                .status(AnalysisStatus.Status.PENDING)
                .build());

        return SaveSelectedRepositoriesResponse.builder()
                .selectedRepositoryId(newSel.getSelectedRepositoryId())
                .message("레포지토리 선택 저장 요청 처리 완료")
                .build();
    }

    /**
     * 현재 사용자의 GitHub 리포지토리 목록을 조회합니다.
     *
     * @return 사용자의 저장된 리포지토리 정보를 담은 {@link GetMyRepositoryResponse}
     * @throws UserNotFoundException 사용자 정보를 찾을 수 없을 때 발생
     */
    @Transactional(readOnly = true)
    public GetMyRepositoryResponse getMyRepository() {
        String userName = authenticationFacade.getCurrentUsername();
        User user = userRepository.findByGithubName(userName)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));
        int userId = user.getId();
        GithubRepository repoDoc = githubRepoRepository.findByUserId(userId)
                .orElseThrow(() -> new GithubRepositoryNotFoundException("Github repository not found"));

        List<Repo> repos = repoDoc.getRepositories().stream()
                .map(r -> Repo.builder().repoId(r.getRepoId()).repoName(r.getRepoName()).build())
                .collect(Collectors.toList());
        return GetMyRepositoryResponse.builder().repositories(repos).build();
    }

    /**
     * 사용자가 저장한 Repository 조합 목록 및 세부 정보를 조회합니다.
     *
     * @return Repository 조합 리스트를 담은 {@link GetMyRepositoryCombinationResponse}
     * @throws UserNotFoundException 사용자 정보를 찾을 수 없을 때 발생
     */
    @Transactional(readOnly = true)
    public GetMyRepositoryCombinationResponse getMyRepositoryCombination() {
        String userName = authenticationFacade.getCurrentUsername();
        User user = userRepository.findByGithubName(userName)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));
        int userId = user.getId();

        List<RepositoryCombination> combs = selectedRepoRepository.findAllByUserId(userId).stream()
                .map(sr -> RepositoryCombination.builder()
                        .selectedRepositoryId(sr.getSelectedRepositoryId())
                        .repositoryNames(sr.getRepositories().stream().map(Repository::getRepoName).collect(Collectors.toList()))
                        .repositoryIds(sr.getRepositories().stream().map(Repository::getRepoId).collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
        return GetMyRepositoryCombinationResponse.builder().repositoryCombinations(combs).build();
    }

    /**
     * 지정된 Repository 조합을 삭제하고 관련 분석 결과도 함께 삭제합니다.
     *
     * @param selectedRepositoryId 삭제할 조합 고유 ID
     * @return 삭제 완료 메시지를 담은 {@link MessageOnlyResponse}
     * @throws UserNotFoundException 사용자 정보를 찾을 수 없을 때 발생
     * @throws GithubRepositoryNotFoundException 조합을 찾을 수 없을 때 발생
     */
    @Transactional
    public MessageOnlyResponse deleteSelectedGithubRepository(String selectedRepositoryId) {
        String userName = authenticationFacade.getCurrentUsername();
        int userId = userRepository.findByGithubName(userName)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND))
                .getId();

        SelectedRepository selRepo = selectedRepoRepository
                .findByUserIdAndSelectedRepositoryId(userId, selectedRepositoryId)
                .orElseThrow(() -> new GithubRepositoryNotFoundException("Github repository combination not found"));

        githubAnalysisResultRepository.findBySelectedRepositoriesId(selectedRepositoryId)
                .ifPresent(githubAnalysisResultRepository::delete);
        selectedRepoRepository.delete(selRepo);

        return MessageOnlyResponse.builder()
                .message("레포지토리 조합과 분석 결과가 삭제되었습니다.")
                .build();
    }

    /**
     * GitHub API를 호출하여 새로운 Repository 생성 이벤트를 확인하고,
     * 감지된 경우 저장된 리포지토리 정보를 업데이트합니다.
     *
     * @return 업데이트 완료 여부 메시지를 담은 {@link MessageOnlyResponse}
     * @throws UserNotFoundException 사용자 정보를 찾을 수 없을 때 발생
     */
    @Transactional
    public MessageOnlyResponse updateGithubData() {
        String userName = authenticationFacade.getCurrentUsername();
        User user = userRepository.findByGithubName(userName)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));
        String token = user.getGithubAccessToken();
        int userId = user.getId();

        boolean isNewRepo = githubRestApiService.checkNewRepositoryCreationEvents(token, userName, userId);
        if (isNewRepo) {
            githubAnalysisService.saveUserGithubRepositoryInfo(token, userName, userId);
            return new MessageOnlyResponse("새로운 Github Repository가 감지되었습니다. 데이터를 업데이트합니다.");
        }
        return new MessageOnlyResponse("감지된 새로운 Github Repository가 없습니다.");
    }

    /**
     * 분석이 완료된 경우 상세 결과를 구성하여 반환합니다.
     *
     * @param selectedRepositoryId 조합 고유 ID
     * @param analysisStatus      분석 상태 엔티티
     * @return 완료된 분석 결과를 담은 {@link GetGithubAnalysisByRepositoryResponse}
     */
    private GithubAnalysisResponse buildCompleteResponse(String selectedRepositoryId, AnalysisStatus analysisStatus) {
        GithubAnalysisResult result = githubAnalysisResultRepository
                .findFirstBySelectedRepositoriesIdOrderByAnalysisDateDesc(selectedRepositoryId)
                .orElseThrow(() -> new GithubAnalysisNotFoundException("Github Analysis Result not found"));

        String overallGrade = calculateOverallScore(result.getOverallScore());
        List<String> names = result.getSelectedRepositories().stream().map(Repository::getRepoName).toList();
        List<Integer> ids = result.getSelectedRepositories().stream().map(Repository::getRepoId).toList();
        String date = result.getAnalysisDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        return GetGithubAnalysisByRepositoryResponse.builder()
                .status(analysisStatus.getStatus().name())
                .selectedRepositoryId(selectedRepositoryId)
                .analysisDate(date)
                .languageRatios(result.getLanguageRatios())
                .selectedRepositories(names)
                .selectedRepositoryIds(ids)
                .overallScore(overallGrade)
                .activityMetrics(result.getActivityMetrics())
                .aiAnalysis(result.getAiAnalysis())
                .build();
    }

    /**
     * 분석이 완료되지 않은 경우 상태 메시지와 기본 정보를 반환합니다.
     *
     * @param selectedRepositoryId 조합 고유 ID
     * @param analysisStatus      분석 상태 엔티티
     * @return 진행 중/실패 상태를 담은 {@link GetGithubAnalysisStatusResponse}
     */
    private GithubAnalysisResponse buildNotCompleteResponse(String selectedRepositoryId, AnalysisStatus analysisStatus) {
        String message = getNotCompleteMessage(analysisStatus.getStatus());
        SelectedRepository selRepo = selectedRepoRepository.findById(selectedRepositoryId)
                .orElseThrow(() -> new GithubRepositoryNotFoundException("Selected repository not found"));
        List<String> names = selRepo.getRepositories().stream().map(Repository::getRepoName).toList();
        List<Integer> ids = selRepo.getRepositories().stream().map(Repository::getRepoId).toList();

        return GetGithubAnalysisStatusResponse.builder()
                .status(analysisStatus.getStatus().name())
                .selectedRepositoryId(selectedRepositoryId)
                .selectedRepositories(names)
                .selectedRepositoryIds(ids)
                .message(message)
                .build();
    }

    /**
     * 점수(0~100)를 등급(A+~D)으로 변환합니다.
     *
     * @param score 원점수
     * @return 변환된 등급 문자열
     */
    private String calculateOverallScore(int score) {
        if (score > 90) return "A+";
        if (score > 80) return "A";
        if (score > 70) return "B+";
        if (score > 60) return "B";
        if (score > 50) return "C+";
        if (score > 40) return "C";
        return "D";
    }

    /**
     * 분석 진행 상태에 따라 사용자에게 표시할 메시지를 반환합니다.
     *
     * @param status 현재 분석 상태
     * @return 상태별 메시지 문자열
     */
    private String getNotCompleteMessage(AnalysisStatus.Status status) {
        return switch (status) {
            case PENDING -> "분석이 아직 시작되지 않았습니다.";
            case ANALYZING -> "분석이 진행 중입니다.";
            case FAIL -> "분석에 실패하였습니다.";
            default -> "분석 상태를 확인할 수 없습니다.";
        };
    }
}
