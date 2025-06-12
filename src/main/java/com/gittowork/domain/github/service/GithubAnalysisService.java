package com.gittowork.domain.github.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gittowork.domain.firebase.service.FirebaseService;
import com.gittowork.domain.github.entity.*;
import com.gittowork.domain.github.model.analysis.ActivityMetrics;
import com.gittowork.domain.github.model.analysis.JavaPenaltyResult;
import com.gittowork.domain.github.model.analysis.RepositoryResult;
import com.gittowork.domain.github.model.analysis.Stats;
import com.gittowork.domain.github.model.commit.Commit;
import com.gittowork.domain.github.model.repository.Repository;
import com.gittowork.domain.github.model.sonar.MeasuresResponse;
import com.gittowork.domain.github.model.sonar.SonarResponse;
import com.gittowork.domain.github.repository.*;
import com.gittowork.domain.user.entity.User;
import com.gittowork.domain.user.repository.UserRepository;
import com.gittowork.global.exception.*;
import com.gittowork.global.service.GithubRestApiService;
import com.gittowork.global.service.GptService;
import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubAnalysisService {

    private final FirebaseService firebaseService;
    private final AnalysisStatusRepository analysisStatusRepository;
    @Value("${sonar.host.url}")
    private String sonarHostUrl;

    @Value("${sonar.analysis.token}")
    private String sonarAnalysisToken;

    @Value("${sonar.user.token}")
    private String sonarUserToken;

    private final UserRepository userRepository;
    private final GithubRestApiService githubRestApiService;
    private final GithubRepoRepository githubRepoRepository;
    private final SelectedRepoRepository selectedRepoRepository;
    private final GptService gptService;
    private final GithubAnalysisResultRepository githubAnalysisResultRepository;
    private final GithubCommitRepository githubCommitRepository;
    private final GithubPullRequestRepository githubPullRequestRepository;
    private final GithubIssueRepository githubIssueRepository;
    private final RestTemplate restTemplate;

    private static final String USER_NOT_FOUND = "User not found";

    private static final String SEVERITY_BLOCKER = "BLOCKER";
    private static final String SEVERITY_CRITICAL = "CRITICAL";
    private static final String SEVERITY_MAJOR = "MAJOR";
    private static final String SEVERITY_MINOR = "MINOR";
    private static final String SEVERITY_INFO = "INFO";

    /**
     * 1. 메서드 설명: 비동기로 선택된 repository에 대해 GitHub 분석을 수행하는 API.
     * 2. 로직:
     *    - username을 기반으로 User 엔티티를 조회하여 userId를 확보한다.
     *    - 확보한 userId와 선택된 repository 배열을 사용하여 분석 로직을 수행한다.
     * 3. param:
     *      int[] selectedRepositories - 분석 대상 repository의 repoId 배열.
     *      String userName - 현재 인증된 사용자의 username.
     * 4. return: 없음 (비동기 작업 수행).
     */
    @Async
    public void githubAnalysisByRepository(int[] selectedRepositories, String userName) {
        User user = userRepository.findByGithubName(userName)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));
        String selectedRepositoryId = analysisSelectedRepositories(user.getId(), selectedRepositories);

        try {
            firebaseService.sendGithubAnalysisMessage(
                    user,
                    "Github 분석 완료",
                    user.getGithubName() + "님, Github 분석이 완료되었습니다. \n 지금 바로 확인하세요!",
                    "GithubAnalysis",
                    selectedRepositoryId);
        } catch (FirebaseMessagingException e) {
            throw new FirebaseMessageException("Firebase message send failed");
        }
    }

    /**
     * 1. 메서드 설명: GitHub API를 통해 사용자 관련 repository, commit, language, issue, pull request 정보를 비동기적으로 조회 및 저장한다.
     * 2. 로직:
     *    - githubRestApiService의 각 메서드를 호출하여 관련 정보를 조회 후 데이터베이스에 저장한다.
     * 3. param:
     *      String accessToken - GitHub API 접근에 사용되는 access token.
     *      String userName - GitHub 사용자 이름.
     *      int userId - 로컬 사용자 식별자.
     * 4. return: 없음.
     */
    @Async
    public void saveUserGithubRepositoryInfo(String accessToken, String userName, int userId) {
        var userGithubRepository = githubRestApiService.saveUserGithubRepository(accessToken, userName, userId);
        githubRestApiService.saveUserGithubCommits(accessToken, userName, userId);
        githubRestApiService.saveUserRepositoryLanguage(accessToken, userName, userId);
        githubRestApiService.saveGithubIssues(accessToken, userName, userId);
        githubRestApiService.saveGithubPullRequests(accessToken, userName, userId);
        githubRestApiService.checkNewGithubEvents(
                accessToken,
                userName,
                userId,
                userGithubRepository.getRepositories().stream().map(Repository::getRepoName).collect(Collectors.toList())
        );
        log.info("{}: Github repository info saved", userName);
    }

    /**
     * 1. 메서드 설명: 선택된 repository들에 대해 SonarQube 분석과 GitHub 관련 정보를 조회하여 최종 분석 결과를 생성 및 저장하며,
     *    분석 도중 예외 발생 시 해당 AnalysisStatus를 fail 상태로 업데이트한다.
     * 2. 로직:
     *    - userId에 해당하는 모든 repository를 조회한 후, 전달받은 selectedRepositoryIds에 해당하는 repository들을 필터링한다.
     *    - 각 repository에 대해 processRepository()를 호출하여 개별 분석 결과를 생성하고, 통계값(언어 비율, 점수, 활동 지표)을 누적한다.
     *    - 누적된 통계값을 기반으로 전체 언어 비율과 평균 점수(ActivityMetrics 포함)를 계산한다.
     *    - 계산된 결과를 바탕으로 GPT 서비스를 이용한 추가 분석을 수행하고, 최종 분석 결과(GithubAnalysisResult)를 생성하여 저장한다.
     *    - 분석이 정상적으로 완료되면, 해당 AnalysisStatus를 complete 상태로 업데이트하며, 도중 Exception이 발생하면 catch 블록에서
     *      AnalysisStatus를 fail 상태로 업데이트한 후 예외를 재전파한다.
     * 3. param:
     *      int userId - 로컬 사용자 식별자.
     *      int[] selectedRepositoryIds - 분석 대상 repository들의 repoId 배열.
     * 4. return: 없음.
     */
    private String analysisSelectedRepositories(int userId, int[] selectedRepositoryIds) {
        GithubRepository githubRepository = githubRepoRepository.findByUserId(userId)
                .orElseThrow(() -> new GithubRepositoryNotFoundException("Github repository not found"));

        Set<Integer> selectedRepoIdSet = Arrays.stream(selectedRepositoryIds)
                .boxed()
                .collect(Collectors.toSet());

        List<Repository> selectedRepositories = githubRepository.getRepositories().stream()
                .filter(repo -> selectedRepoIdSet.contains(repo.getRepoId()))
                .collect(Collectors.toList());

        SelectedRepository selectedRepository = selectedRepoRepository.findByUserIdAndRepositories(userId, selectedRepositories)
                .orElseThrow(() -> new GithubRepositoryNotFoundException("Github repository not found"));

        Map<String, Integer> totalLanguageRatio = new HashMap<>();
        AtomicInteger totalOverallScore = new AtomicInteger(0);
        AtomicInteger totalStars = new AtomicInteger(0);
        AtomicInteger totalCommits = new AtomicInteger(0);
        AtomicInteger totalPRs = new AtomicInteger(0);
        AtomicInteger totalIssues = new AtomicInteger(0);

        try {
            List<RepositoryResult> repositoryResults = selectedRepository.getRepositories().stream()
                    .map(repo -> processRepository(repo, totalLanguageRatio, totalOverallScore, totalStars, totalCommits, totalPRs, totalIssues))
                    .collect(Collectors.toList());

            int totalLines = totalLanguageRatio.values().stream().mapToInt(Integer::intValue).sum();
            Map<String, Double> languagePercentages = totalLanguageRatio.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> totalLines > 0 ? (entry.getValue() * 100.0 / totalLines) : 0.0
                    ));

            int overallScoreMean = selectedRepository.getRepositories().isEmpty() ? 0 :
                    totalOverallScore.get() / selectedRepository.getRepositories().size();

            ActivityMetrics activityMetrics = ActivityMetrics.builder()
                    .totalStars(totalStars.get())
                    .totalCommits(totalCommits.get())
                    .totalPRs(totalPRs.get())
                    .totalIssues(totalIssues.get())
                    .build();

            GithubAnalysisResult githubAnalysisResult = GithubAnalysisResult.builder()
                    .userId(userId)
                    .analysisDate(LocalDateTime.now())
                    .selectedRepositoriesId(selectedRepository.getSelectedRepositoryId())
                    .selectedRepositories(selectedRepository.getRepositories())
                    .languageRatios(languagePercentages)
                    .repositories(repositoryResults)
                    .overallScore(overallScoreMean)
                    .primaryRole(null)
                    .roleScores(0)
                    .activityMetrics(activityMetrics)
                    .aiAnalysis(null)
                    .build();

            GithubAnalysisResult updatedResult  = getGptAnalysis(githubAnalysisResult);

            githubAnalysisResultRepository.save(updatedResult);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));

            AnalysisStatus analysisStatus = analysisStatusRepository.findByUserAndSelectedRepositoriesId(user, selectedRepository.getSelectedRepositoryId())
                    .orElseThrow(() -> new GithubAnalysisNotFoundException("Github analysis status not found"));

            analysisStatus.setStatus(AnalysisStatus.Status.COMPLETE);
            analysisStatusRepository.save(analysisStatus);

            return analysisStatus.getSelectedRepositoriesId();

        } catch (Exception e) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));
            AnalysisStatus analysisStatus = analysisStatusRepository.findByUserAndSelectedRepositoriesId(user, selectedRepository.getSelectedRepositoryId())
                    .orElseThrow(() -> new GithubAnalysisNotFoundException("Github analysis status not found"));
            analysisStatus.setStatus(AnalysisStatus.Status.FAIL);
            analysisStatusRepository.save(analysisStatus);
            throw e;
        }
    }

    /**
     * 1. 메서드 설명: 주어진 GithubAnalysisResult 객체에 대해 GPT 분석을 수행하여 primaryRole, roleScores, aiAnalysis 값을 업데이트한다.
     * 2. 로직:
     *    - gptService의 githubDataAnalysis 메서드를 호출하여 GPT 분석 결과를 받아온다.
     *    - GPT 분석 결과로부터 primaryRole, roleScores, aiAnalysis 값을 추출하여 원본 GithubAnalysisResult 객체에 설정한다.
     *    - 업데이트된 객체를 로그에 기록한 후 반환한다.
     * 3. param:
     *      GithubAnalysisResult githubAnalysisResult - GPT 분석 전의 GithubAnalysisResult 객체.
     * 4. return: primaryRole, roleScores, aiAnalysis가 업데이트된 GithubAnalysisResult 객체.
     */
    private GithubAnalysisResult getGptAnalysis(GithubAnalysisResult githubAnalysisResult) {
        try {
            GithubAnalysisResult gptAnalysisResult = gptService.githubDataAnalysis(githubAnalysisResult, 500);
            githubAnalysisResult.setPrimaryRole(gptAnalysisResult.getPrimaryRole());
            githubAnalysisResult.setRoleScores(gptAnalysisResult.getRoleScores());
            githubAnalysisResult.setAiAnalysis(gptAnalysisResult.getAiAnalysis());
            log.info("GithubAnalysisResult with gptAnalysisResult: {}", githubAnalysisResult);
            return githubAnalysisResult;
        } catch (JsonProcessingException e) {
            throw new GithubAnalysisException("Github analysis failed: " + e.getMessage());
        }
    }

    /**
     * 1. 메서드 설명: 단일 repository에 대해 SonarQube 분석과 GitHub 커밋/PR/Issue 정보를 조회하여 RepositoryResult를 생성한다.
     * 2. 로직:
     *    - repository를 클론하고 projectKey를 추출한 후, SonarQube 분석을 위한 스캐너를 실행한다.
     *    - 분석 결과를 pollAndParseAnalysisResult()로 받아오고, GitHub 관련 통계(커밋, PR, Issue, 언어 분포)를 계산하여 RepositoryResult를 생성한다.
     * 3. param:
     *      Repository repository - 분석 대상 repository.
     *      Map<String, Integer> totalLanguageRatio - 전체 언어 분포 누적 Map.
     *      AtomicInteger totalOverallScore - 전체 점수 누적 변수.
     *      AtomicInteger totalStars - 전체 star 수 누적 변수.
     *      AtomicInteger totalCommits - 전체 commit 수 누적 변수.
     *      AtomicInteger totalPRs - 전체 pull request 수 누적 변수.
     *      AtomicInteger totalIssues - 전체 issue 수 누적 변수.
     * 4. return: RepositoryResult 객체.
     */
    private RepositoryResult processRepository(Repository repository,
                                               Map<String, Integer> totalLanguageRatio,
                                               AtomicInteger totalOverallScore,
                                               AtomicInteger totalStars,
                                               AtomicInteger totalCommits,
                                               AtomicInteger totalPRs,
                                               AtomicInteger totalIssues) {
        String repositoryPathUrl = "https://github.com/" + repository.getFullName() + ".git";
        try {
            File localRepo = cloneRepository(repositoryPathUrl);
            String projectKey = extractProjectKey(repositoryPathUrl);

            ProcessBuilder processBuilder = getProcessBuilder(repository, projectKey, localRepo);
            processBuilder.directory(localRepo);

            Process process = processBuilder.start();

            Thread stdoutThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    reader.lines().forEach(log::info);
                } catch (IOException e) {
                    log.error("Error reading stdout", e);
                }
            });
            Thread stderrThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    reader.lines().forEach(log::error);
                } catch (IOException e) {
                    log.error("Error reading stderr", e);
                }
            });
            stdoutThread.start();
            stderrThread.start();

            int exitCode = process.waitFor();
            stdoutThread.join();
            stderrThread.join();

            if (exitCode != 0) {
                log.info("Github analysis exited with exit code: {}", exitCode);
                throw new SonarAnalysisException("SonarQube analysis failed for project: " + repositoryPathUrl);
            }

            RepositoryResult result = pollAndParseAnalysisResult(projectKey, repository.getRepoId());

            GithubCommit githubCommit = githubCommitRepository.findByRepoId(repository.getRepoId())
                    .orElseThrow(() -> new GithubRepositoryNotFoundException("Github commit repository not found"));
            List<GithubPullRequest> githubPullRequests = githubPullRequestRepository.findAllByRepoId(repository.getRepoId())
                    .orElseThrow(() -> new GithubRepositoryNotFoundException("Github pull requests not found"));
            List<GithubIssue> githubIssues = githubIssueRepository.findAllByRepoId(repository.getRepoId())
                    .orElseThrow(() -> new GithubRepositoryNotFoundException("Github issues not found"));

            int commitCount = Optional.ofNullable(githubCommit.getCommits()).map(List::size).orElse(0);
            int prCount = githubPullRequests.size();
            int issueCount = githubIssues.size();

            Stats stats = Stats.builder()
                    .stargazersCount(repository.getStargazersCount())
                    .commitCount(commitCount)
                    .prCount(prCount)
                    .issueCount(issueCount)
                    .build();
            result.setStats(stats);

            List<Commit> commits = githubCommit.getCommits();
            commits.sort(Comparator.comparing(Commit::getCommitDate).reversed());
            LocalDateTime latestDate = commits.get(0).getCommitDate();
            LocalDateTime oldestDate = commits.get(commits.size() - 1).getCommitDate();
            int daysDifference = (int) ChronoUnit.DAYS.between(oldestDate, latestDate);
            double commitFrequency = daysDifference > 0 ? (double) commitCount / daysDifference : commitCount;
            result.setCommitFrequency(commitFrequency);

            result.getLanguages().forEach((lang, count) ->
                    totalLanguageRatio.merge(lang, count, Integer::sum)
            );

            totalOverallScore.addAndGet(result.getScore());
            totalStars.addAndGet(repository.getStargazersCount());
            totalCommits.addAndGet(commitCount);
            totalPRs.addAndGet(prCount);
            totalIssues.addAndGet(issueCount);
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while analyzing repository: {}", repositoryPathUrl, e);
            throw new SonarAnalysisException("SonarQube analysis failed due to interruption: " + e.getMessage());
        } catch (IOException e) {
            log.error("IOException while analyzing repository: {}", repositoryPathUrl, e);
            throw new SonarAnalysisException("SonarQube analysis failed: " + e.getMessage());
        }
    }

    /**
     * 1. 메서드 설명: 주어진 repository, projectKey, 로컬 디렉토리를 기반으로 SonarQube 및 PMD 분석 명령을 실행할 ProcessBuilder를 생성한다.
     * 2. 로직:
     *    - 분석에 필요한 명령어 문자열을 구성한 후, bash -c 명령을 통해 ProcessBuilder를 생성한다.
     * 3. param:
     *      Repository repository - 분석 대상 repository.
     *      String projectKey - SonarQube 프로젝트 키.
     *      File localRepo - 클론된 로컬 repository 디렉토리.
     * 4. return: ProcessBuilder 객체.
     */
    private ProcessBuilder getProcessBuilder(Repository repository, String projectKey, File localRepo) {
        String command = String.format(
                "mkdir -p /pmd_result/%s ; " +
                        "pmd check -d \"%s\" -R rulesets/java/quickstart.xml -f xml -r /pmd_result/%s/pmd-report.xml ; " +
                        "python3 /app/scripts/pmd_to_sonar.py /pmd_result/%s/pmd-report.xml /pmd_result/%s/pmd-report.json ; " +
                        "sonar-scanner -X -Dsonar.log.level=TRACE " +
                        "-Dsonar.projectBaseDir=\"%s\" " +
                        "-Dsonar.projectKey=%s " +
                        "-Dsonar.projectName=\"%s\" " +
                        "-Dsonar.sources=. " +
                        "-Dsonar.host.url=%s " +
                        "-Dsonar.login=%s " +
                        "-Dsonar.exclusions=**/*.java " +
                        "-Dsonar.externalIssuesReportPaths=/pmd_result/%s/pmd-report.json",
                projectKey, localRepo.getAbsolutePath(), projectKey,
                projectKey, projectKey,
                localRepo.getAbsolutePath(),
                projectKey,
                repository.getFullName(),
                sonarHostUrl,
                sonarAnalysisToken,
                projectKey
        );
        return new ProcessBuilder("bash", "-c", command);
    }

    /**
     * 1. 메서드 설명: 주어진 repository URL을 기반으로 로컬에 repository를 클론한다.
     * 2. 로직:
     *    - URL에서 projectKey를 추출한 후, 지정된 경로에 디렉토리가 없으면 JGit을 사용하여 클론한다.
     * 3. param:
     *      String repoUrl - 클론할 repository의 URL.
     * 4. return: 클론된 로컬 repository의 File 객체.
     */
    private File cloneRepository(String repoUrl) {
        String projectKey = extractProjectKey(repoUrl);
        File repoDir = new File("/tmp/repositories/" + projectKey);
        if (!repoDir.exists()) {
            try {
                Git.cloneRepository()
                        .setURI(repoUrl)
                        .setDirectory(repoDir)
                        .call();
            } catch (GitAPIException e) {
                log.error("Error while cloning repository: {}", repoUrl, e);
                throw new SonarAnalysisException("Failed to clone repository: " + e.getMessage());
            }
        }
        return repoDir;
    }

    /**
     * 1. 메서드 설명: 주어진 repository URL에서 organization과 project 이름을 추출하여 프로젝트 키를 생성한다.
     * 2. 로직:
     *    - URL을 "/"로 분리한 후, 마지막 두 부분(organization, project)을 결합하여 projectKey를 생성한다.
     * 3. param:
     *      String repoUrl - repository의 URL.
     * 4. return: 생성된 projectKey 문자열.
     */
    private String extractProjectKey(String repoUrl) {
        String[] parts = repoUrl.split("/");
        String org = parts[parts.length - 2];
        String project = parts[parts.length - 1].replace(".git", "");
        return org + "_" + project;
    }

    /**
     * 1. 메서드 설명: SonarQube와 PMD 분석 결과 및 GitHub 데이터를 통합하여 최종 RepositoryResult를 생성한다.
     * 2. 로직:
     *    - SonarQube API를 호출하여 비자바 메트릭 기반 penalty를 계산하고, 언어 분포 정보를 조회한다.
     *    - 로컬 repository에서 Java 파일의 총 라인 수(ncloc)를 계산하여 언어 분포에 추가한다.
     *    - PMD API를 통해 자바 penalty를 계산하고, 이를 기반으로 자바 품질 점수를 산출한 후 최종 점수를 계산한다.
     * 3. param:
     *      String projectKey - SonarQube 프로젝트 키.
     *      int repoId - 분석 대상 repository의 식별자.
     * 4. return: RepositoryResult 객체.
     */
    private RepositoryResult pollAndParseAnalysisResult(String projectKey, int repoId) {
        Map<String, Double> weights = Map.of(
                "coverage", 8.0,
                "bugs", 16.0,
                "code_smells", 12.0,
                "vulnerabilities", 20.0,
                "duplicated_lines_density", 4.0
        );
        final int BASE_SCORE = 100;
        double sonarTotalPenalty = 0.0;

        SonarResponse sonarResponse = fetchAnalysisResult(projectKey);
        if (sonarResponse == null || sonarResponse.getComponent() == null) {
            throw new SonarAnalysisException("Failed to fetch analysis result.");
        }

        for (SonarResponse.Measure measure : sonarResponse.getComponent().getMeasures()) {
            String metric = measure.getMetric();
            double weight = weights.getOrDefault(metric, 10.0);
            double value;
            try {
                value = Double.parseDouble(measure.getValue());
            } catch (NumberFormatException e) {
                continue;
            }

            double penalty = switch (metric) {
                case "coverage" -> weight * ((100.0 - value) / 100.0);
                case "duplicated_lines_density" -> weight * (value / 100.0);
                case "bugs", "code_smells", "vulnerabilities" -> weight * Math.min(1.0, Math.log10(value + 1) / 2.0);
                default -> 0.0;
            };
            sonarTotalPenalty += penalty;
        }

        int nonJavaScore = (int) Math.max(0, BASE_SCORE - sonarTotalPenalty);

        Map<String, Double> languageDistribution = new HashMap<>(fetchLanguageDistribution(projectKey));
        File repoDir = new File("/tmp/repositories/" + projectKey);
        double javaLoc = calculateJavaNcloc(repoDir);
        if (javaLoc > 0) {
            languageDistribution.put("java", javaLoc);
        }
        Map<String, Integer> languageDistributionInt = languageDistribution.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().intValue()));

        Map<String, String> projectMeasures = fetchProjectMeasures(projectKey);

        JavaPenaltyResult javaPenaltyResult = calculateJavaPenalty(projectKey);
        double javaPenalty = javaPenaltyResult.getPenalty();
        double javaQualityScore = Math.max(0, 100 - javaPenalty);
        int overallScore = (int) Math.max(0, nonJavaScore - javaPenalty);

        String insights = String.format("""
                    Non-Java Analysis:
                      - Base Score (from SonarQube analysis): 100 - total penalty (%.2f) = %d
                    Java Analysis (via PMD):
                      - BLOCKER: %d violations, CRITICAL: %d violations, MAJOR: %d violations, MINOR: %d violations, INFO: %d violations
                      - Total Java PMD penalty: %.2f => Java Quality Score: 100 - penalty = %.2f
                    Overall Score: Non-Java Score (%d) - Java PMD penalty (%.2f) = %d
                    Language Distribution (LOC): %s
                    """,
                sonarTotalPenalty, nonJavaScore,
                javaPenaltyResult.getBlockerCount(), javaPenaltyResult.getCriticalCount(),
                javaPenaltyResult.getMajorCount(), javaPenaltyResult.getMinorCount(), javaPenaltyResult.getInfoCount(),
                javaPenalty, javaQualityScore,
                nonJavaScore, javaPenalty, overallScore,
                languageDistributionInt
        );

        return RepositoryResult.builder()
                .repoId(repoId)
                .score(overallScore)
                .insights(insights)
                .languages(languageDistributionInt)
                .stats(null)
                .projectMeasures(projectMeasures)
                .build();
    }


    /**
     * 1. 메서드 설명: PMD 이슈 데이터를 조회하여 자바 코드에 대한 penalty와 violation 카운터를 계산한다. (로그 스케일 적용)
     * 2. 로직:
     *    - SonarQube API를 호출하여 PMD 관련 이슈 데이터를 조회한 후, 각 심각도별 건수를 집계하고, 로그 함수를 적용하여 penalty를 산출한다.
     * 3. param:
     *      String projectKey - SonarQube 프로젝트 키.
     * 4. return: JavaPenaltyResult 객체 (penalty와 각 violation 카운터 포함).
     */
    private JavaPenaltyResult calculateJavaPenalty(String projectKey) {
        String pmdIssuesUrl = sonarHostUrl + "/api/issues/search?componentKeys=" + projectKey + "&engineId=pmd";
        HttpEntity<String> request = setHttpRequest(sonarAnalysisToken);
        ResponseEntity<Map<String, Object>> issuesResponse = restTemplate.exchange(
                pmdIssuesUrl,
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        int blockerCount = 0;
        int criticalCount = 0;
        int majorCount = 0;
        int minorCount = 0;
        int infoCount = 0;
        if (issuesResponse.getBody() != null && issuesResponse.getBody().containsKey("issues")) {
            List<Map<String, Object>> issues = (List<Map<String, Object>>) issuesResponse.getBody().get("issues");
            for (Map<String, Object> issue : issues) {
                String severity = (String) issue.get("severity");
                switch (severity) {
                    case SEVERITY_BLOCKER -> blockerCount++;
                    case SEVERITY_CRITICAL -> criticalCount++;
                    case SEVERITY_MAJOR -> majorCount++;
                    case SEVERITY_MINOR -> minorCount++;
                    case SEVERITY_INFO -> infoCount++;
                    default -> log.warn("Unexpected severity encountered: {}", severity);
                }
            }
        }

        Map<String, Double> severityWeights = Map.of(
                SEVERITY_BLOCKER, 6.0,
                SEVERITY_CRITICAL, 4.0,
                SEVERITY_MAJOR, 2.4,
                SEVERITY_MINOR, 1.0,
                SEVERITY_INFO, 0.4
        );

        double javaPenalty = 0.0;
        javaPenalty += severityWeights.get(SEVERITY_BLOCKER) * Math.log((double) blockerCount + 1);
        javaPenalty += severityWeights.get(SEVERITY_CRITICAL) * Math.log((double) criticalCount + 1);
        javaPenalty += severityWeights.get(SEVERITY_MAJOR) * Math.log((double) majorCount + 1);
        javaPenalty += severityWeights.get(SEVERITY_MINOR) * Math.log((double) minorCount + 1);
        javaPenalty += severityWeights.get(SEVERITY_INFO) * Math.log((double) infoCount + 1);

        return JavaPenaltyResult.builder()
                .penalty(javaPenalty)
                .blockerCount(blockerCount)
                .criticalCount(criticalCount)
                .majorCount(majorCount)
                .minorCount(minorCount)
                .infoCount(infoCount)
                .build();
    }

    /**
     * 1. 메서드 설명: 지정된 repository 디렉토리 내의 모든 Java 소스 파일(.java)의 총 라인 수(ncloc)를 계산한다.
     * 2. 로직:
     *    - Files.walk()를 사용하여 디렉토리 하위의 모든 .java 파일을 탐색한 후, 각 파일의 라인 수를 합산한다.
     * 3. param:
     *      File repoDir - Java 소스 파일들이 포함된 repository의 루트 디렉토리.
     * 4. return: 모든 Java 파일의 총 라인 수 (double).
     */
    private double calculateJavaNcloc(File repoDir) {
        if (!repoDir.exists() || !repoDir.isDirectory()) {
            log.warn("Repository directory {} does not exist or is not a directory.", repoDir.getAbsolutePath());
            return 0.0;
        }

        try (Stream<Path> paths = Files.walk(repoDir.toPath())) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .mapToLong(path -> {
                        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
                        decoder.onMalformedInput(CodingErrorAction.IGNORE);

                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(Files.newInputStream(path), decoder))) {
                            return reader.lines().count();
                        } catch (IOException e) {
                            log.error("Error reading file {} (skipping file): {}", path, e.getMessage());
                            return 0L;
                        }
                    }).sum();
        } catch (IOException e) {
            log.error("Error walking through repository directory {}: {}", repoDir.getAbsolutePath(), e.getMessage());
        }
        return 0.0;
    }


    /**
     * 1. 메서드 설명: SonarQube API를 호출하여 지정된 프로젝트의 측정 지표(coverage, bugs, code_smells, vulnerabilities, duplicated_lines_density)를 조회한다.
     * 2. 로직:
     *    - 주어진 projectKey를 사용하여 API URL을 구성하고, 인증 헤더를 포함한 GET 요청을 수행한 후 SonarResponse로 파싱한다.
     * 3. param:
     *      String projectKey - SonarQube 프로젝트 키.
     * 4. return: SonarResponse 객체.
     */
    private SonarResponse fetchAnalysisResult(String projectKey) {
        String url = sonarHostUrl + "/api/measures/component?component=" + projectKey +
                "&metricKeys=coverage,bugs,code_smells,vulnerabilities,duplicated_lines_density";
        HttpEntity<String> request = setHttpRequest(sonarUserToken);
        ResponseEntity<SonarResponse> response = restTemplate.exchange(url, HttpMethod.GET, request, SonarResponse.class);
        return response.getBody();
    }

    /**
     * 1. 메서드 설명: SonarQube API를 호출하여 프로젝트의 ncloc_language_distribution 측정 지표를 조회한 후 언어별 라인 수 분포 Map으로 반환한다.
     * 2. 로직:
     *    - API 호출 후 응답에서 "ncloc_language_distribution" 측정값을 파싱하여 각 언어별 LOC를 Map에 저장한다.
     * 3. param:
     *      String projectKey - SonarQube 프로젝트 키.
     * 4. return: 언어별 LOC 분포를 나타내는 Map<String, Double>.
     */
    private Map<String, Double> fetchLanguageDistribution(String projectKey) {
        String url = sonarHostUrl + "/api/measures/search?projectKeys=" + projectKey +
                "&metricKeys=ncloc_language_distribution";
        HttpEntity<String> request = setHttpRequest(sonarAnalysisToken);

        ResponseEntity<MeasuresResponse> response = restTemplate.exchange(url, HttpMethod.GET, request, MeasuresResponse.class);
        MeasuresResponse measuresResponse = response.getBody();

        if (measuresResponse != null && measuresResponse.getMeasures() != null) {
            return measuresResponse.getMeasures().stream()
                    .filter(measure -> "ncloc_language_distribution".equals(measure.getMetric()))
                    .findFirst()
                    .map(measure -> {
                        Map<String, Double> languageDistribution = new HashMap<>();
                        String[] entries = measure.getValue().split(";");
                        for (String entry : entries) {
                            String[] parts = entry.split("=");
                            if (parts.length == 2) {
                                try {
                                    languageDistribution.put(parts[0], Double.parseDouble(parts[1]));
                                } catch (NumberFormatException e) {
                                    log.error("Error parsing value for key {}: {}", parts[0], e.getMessage());
                                }
                            }
                        }
                        return languageDistribution;
                    }).orElse(Collections.emptyMap());
        }
        return Collections.emptyMap();
    }

    /**
     * 1. 메서드 설명: SonarQube API를 호출하여 지정된 프로젝트의 주요 측정 지표(coverage, bugs, code_smells, vulnerabilities, duplicated_lines_density)를 조회한 후 Map으로 반환한다.
     * 2. 로직:
     *    - projectKey와 metricKeys를 이용해 API URL을 구성하고, 응답에서 각 측정값을 Map에 저장한다.
     * 3. param:
     *      String projectKey - SonarQube 프로젝트 키.
     * 4. return: 측정 지표를 나타내는 Map<String, String>.
     */
    private Map<String, String> fetchProjectMeasures(String projectKey) {
        String metricKeys = "coverage,bugs,code_smells,vulnerabilities,duplicated_lines_density";
        String url = sonarHostUrl + "/api/measures/search?projectKeys=" + projectKey + "&metricKeys=" + metricKeys;
        HttpEntity<String> request = setHttpRequest(sonarAnalysisToken);

        ResponseEntity<MeasuresResponse> response = restTemplate.exchange(url, HttpMethod.GET, request, MeasuresResponse.class);
        MeasuresResponse measuresResponse = response.getBody();
        Map<String, String> measuresMap = new HashMap<>();
        if (measuresResponse != null && measuresResponse.getMeasures() != null) {
            measuresResponse.getMeasures().forEach(measure ->
                    measuresMap.put(measure.getMetric(), measure.getValue())
            );
        }
        return measuresMap;
    }

    /**
     * 1. 메서드 설명: 주어진 토큰을 이용해 Basic 인증 헤더가 설정된 HttpEntity를 생성하여 반환하는 유틸리티 메서드.
     * 2. 로직:
     *    - 토큰 문자열에 ":"를 추가하여 Basic 인증 형식에 맞게 변환한다.
     *    - 이를 Base64로 인코딩한 후, "Authorization" 헤더에 "Basic " 접두어와 함께 설정한다.
     *    - 설정된 HttpHeaders를 포함하는 HttpEntity 객체를 생성하여 반환한다.
     * 3. param:
     *      String token - Basic 인증에 사용되는 토큰 문자열.
     * 4. return: HttpEntity<String> 객체 (인증 헤더가 포함된 HTTP 요청 엔티티).
     */
    private HttpEntity<String> setHttpRequest(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " +
                Base64.getEncoder().encodeToString((token + ":").getBytes()));

        return new HttpEntity<>(headers);
    }
}
