package com.gittowork.global.service;

import com.gittowork.domain.github.entity.*;
import com.gittowork.domain.github.model.commit.Commit;
import com.gittowork.domain.github.model.event.Event;
import com.gittowork.domain.github.model.issue.IssueLabel;
import com.gittowork.domain.github.model.issue.IssueUser;
import com.gittowork.domain.github.model.pullrequest.PullRequestBranch;
import com.gittowork.domain.github.model.pullrequest.PullRequestUser;
import com.gittowork.domain.github.model.repository.Repository;
import com.gittowork.domain.github.repository.*;
import com.gittowork.global.exception.GithubRepositoryNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GithubRestApiService {

    private final RestTemplate restTemplate;
    private final GithubRepoRepository githubRepoRepository;
    private final GithubCommitRepository githubCommitRepository;
    private final GithubLanguageRepository githubLanguageRepository;
    private final GithubIssueRepository githubIssueRepository;
    private final GithubPullRequestRepository githubPullRequestRepository;
    private final GithubEventRepository githubEventRepository;

    @Value("${github.client.id}")
    private String clientId;

    @Value("${github.client.secret}")
    private String clientSecret;

    @Value("${github.redirect.uri}")
    private String redirectUri;

    @Autowired
    public GithubRestApiService(RestTemplate restTemplate,
                                GithubRepoRepository githubRepoRepository,
                                GithubCommitRepository githubCommitRepository,
                                GithubLanguageRepository githubLanguageRepository,
                                GithubIssueRepository githubIssueRepository,
                                GithubPullRequestRepository githubPullRequestRepository,
                                GithubEventRepository githubEventRepository) {
        this.restTemplate = restTemplate;
        this.githubRepoRepository = githubRepoRepository;
        this.githubCommitRepository = githubCommitRepository;
        this.githubLanguageRepository = githubLanguageRepository;
        this.githubIssueRepository = githubIssueRepository;
        this.githubPullRequestRepository = githubPullRequestRepository;
        this.githubEventRepository = githubEventRepository;
    }

    // ============================================================
    // 1. 인증 및 사용자 정보 관련 메서드
    // ============================================================

    /**
     * 1. 메서드 설명: GitHub OAuth 인증 과정에서 전달받은 code를 사용하여 access token 정보를 가져오는 API.
     * 2. 로직:
     *    - 요청 헤더에 JSON 형식의 응답을 수락하도록 설정하고, 요청 본문에 client_id, client_secret, redirect_uri, code를 추가한다.
     *    - GitHub OAuth access token URL에 POST 요청을 보내 응답 상태 코드가 2xx가 아니면 예외를 발생시키고,
     *      2xx인 경우 응답 본문이 null이면 빈 Map으로 처리한 후 access token 정보를 반환한다.
     * 3. param: code - GitHub OAuth 인증 코드.
     * 4. return: GitHub access token 정보를 포함한 Map 객체.
     */
    public Map<String, Object> getAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        String accessTokenUrl = "https://github.com/login/oauth/access_token";
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                accessTokenUrl,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new GithubRepositoryNotFoundException("Failed to get access token - HTTP " + response.getStatusCode());
        }
        Map<String, Object> bodyResponse = response.getBody();
        return bodyResponse == null ? Collections.emptyMap() : bodyResponse;
    }

    /**
     * 1. 메서드 설명: GitHub API를 호출하여 access token 기반으로 사용자 정보를 조회하는 API.
     * 2. 로직:
     *    - 요청 헤더에 bearer 토큰 방식으로 access token을 설정하고, JSON 형식의 응답을 수락하도록 설정한다.
     *    - GitHub 사용자 정보 API에 GET 요청을 보내 응답 상태가 2xx가 아니면 예외를 발생시키며,
     *      2xx인 경우 응답 본문이 null이면 빈 Map으로 처리한 후 사용자 정보를 반환한다.
     * 3. param: accessToken - GitHub API 접근에 사용되는 access token.
     * 4. return: GitHub 사용자 정보를 포함한 Map 객체.
     */
    public Map<String, Object> getUserInfo(String accessToken) {
        HttpEntity<String> request = new HttpEntity<>(createHeaders(accessToken, MediaType.APPLICATION_JSON));
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "https://api.github.com/user",
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new GithubRepositoryNotFoundException("Failed to get user info - HTTP " + response.getStatusCode());
        }
        Map<String, Object> bodyResponse = response.getBody();
        return bodyResponse == null ? Collections.emptyMap() : bodyResponse;
    }

    // ============================================================
    // 2. Repository 관련 메서드
    // ============================================================

    /**
     * 1. 메서드 설명: GitHub API를 호출하여 사용자의 repository 목록을 조회하고,
     *    userId를 기준으로 기존에 DB에 저장된 repository들과 비교하여, 중복되지 않는 신규 repository만을 추가(중복 방지)한 후,
     *    GithubRepository Document를 저장하는 메서드.
     * 2. 로직:
     *    - accessToken과 githubName을 사용하여 "https://api.github.com/users/{githubName}/repos" 엔드포인트에 GET 요청을 보낸다.
     *    - API 응답이 2xx가 아니면 예외를 발생시키며, 응답 본문이 null이면 빈 리스트로 처리한다.
     *    - 응답 데이터를 Repository 객체 리스트로 매핑한다.
     *    - DB에서 userId에 해당하는 GithubRepository 문서가 존재하는지 확인한다.
     *         - 존재하는 경우: 기존 repositories 목록에서 repoName을 기준으로 중복을 제거하고,
     *           신규 repository만 추가한 후 업데이트(save)한다.
     *         - 존재하지 않는 경우: 새로운 GithubRepository Document를 생성하여 삽입한다.
     * 3. param:
     *      String accessToken - GitHub API 접근에 사용되는 access token.
     *      String githubName  - GitHub 사용자 이름.
     *      int userId         - 현재 애플리케이션 사용자의 로컬 식별자.
     * 4. return: GithubRepository - 저장된 GithubRepository Document.
     */
    public GithubRepository saveUserGithubRepository(String accessToken, String githubName, int userId) {
        HttpEntity<String> request = new HttpEntity<>(createHeaders(accessToken, MediaType.APPLICATION_JSON));
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                "https://api.github.com/users/{githubName}/repos",
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                githubName
        );
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new GithubRepositoryNotFoundException("Failed to fetch repositories - HTTP " + response.getStatusCode());
        }
        List<Map<String, Object>> responseBody = response.getBody();
        List<Repository> newRepositories = (responseBody == null ? Collections.emptyList() : responseBody)
                .stream()
                .map(obj -> {
                    Map<String, Object> map = (Map<String, Object>) obj;
                    return Repository.builder()
                            .repoId((Integer) map.get("id"))
                            .repoName(map.get("name").toString())
                            .fullName(map.get("full_name").toString())
                            .language(map.get("language") != null ? map.get("language").toString() : null)
                            .stargazersCount((Integer) map.get("stargazers_count"))
                            .forksCount((Integer) map.get("forks_count"))
                            .createdAt(OffsetDateTime.parse(map.get("created_at").toString()).toLocalDateTime())
                            .updatedAt(OffsetDateTime.parse(map.get("updated_at").toString()).toLocalDateTime())
                            .pushedAt(OffsetDateTime.parse(map.get("pushed_at").toString()).toLocalDateTime())
                            .description(map.get("description") != null ? map.get("description").toString() : "")
                            .build();
                })
                .collect(Collectors.toList());

        Optional<GithubRepository> optionalGithubRepo = githubRepoRepository.findByUserId(userId);
        if (optionalGithubRepo.isPresent()) {
            GithubRepository existingRepo = optionalGithubRepo.get();

            Set<String> existingRepoNames = existingRepo.getRepositories().stream()
                    .map(Repository::getRepoName)
                    .collect(Collectors.toSet());

            List<Repository> repositoriesToAdd = newRepositories.stream()
                    .filter(repo -> !existingRepoNames.contains(repo.getRepoName()))
                    .toList();

            List<Repository> mergedRepositories = new ArrayList<>(existingRepo.getRepositories());
            mergedRepositories.addAll(repositoriesToAdd);
            existingRepo.setRepositories(mergedRepositories);
            return githubRepoRepository.save(existingRepo);
        } else {
            GithubRepository githubRepository = GithubRepository.builder()
                    .userId(userId)
                    .repositories(newRepositories)
                    .build();
            return githubRepoRepository.save(githubRepository);
        }
    }

    // ============================================================
    // 3. Commit 관련 메서드
    // ============================================================

    /**
     * 1. 메서드 설명: GitHub API를 호출하여 사용자의 각 repository에 대한 커밋 정보를 조회하고,
     *    각 repository별로 기존 DB에 저장된 커밋(Document)와 비교하여, commitSha 기준으로 신규 커밋만 추가(중복 방지)하는 메서드.
     * 2. 로직:
     *    - accessToken과 githubName을 사용하여 각 repository의 커밋 정보를 조회한다.
     *    - API 응답이 2xx가 아니면 예외를 발생시키며, 응답 본문이 null이면 빈 리스트로 처리한다.
     *    - 각 repository에 대해 기존 DB에 저장된 GithubCommit Document가 있는지 확인한다.
     *         * 존재하는 경우: 기존 Document의 commitSha들을 기준으로 신규 커밋만 필터링하여 추가한 후 업데이트한다.
     *         * 존재하지 않는 경우: 조회된 커밋 전체를 포함하는 새로운 GithubCommit Document를 생성하여 저장한다.
     * 3. param:
     *      accessToken - GitHub API 접근에 사용되는 access token.
     *      githubName  - GitHub 사용자 이름.
     *      userId      - 현재 애플리케이션 사용자의 로컬 식별자.
     * 4. return: 없음.
     */
    public void saveUserGithubCommits(String accessToken, String githubName, int userId) {
        HttpEntity<String> request = new HttpEntity<>(createHeaders(accessToken, MediaType.APPLICATION_JSON));
        HttpEntity<String> detailRequest = new HttpEntity<>(createHeaders(accessToken, MediaType.valueOf("application/vnd.github.v3+json")));

        // DB에서 해당 userId의 GithubRepository Document를 조회하여 연관 repository 목록 가져오기
        List<Repository> repositories = githubRepoRepository.findByUserId(userId)
                .orElseThrow(() -> new GithubRepositoryNotFoundException("Github repository not found"))
                .getRepositories();

        // 각 repository에 대해 커밋 정보를 가져와서 중복 여부를 판단 후 저장
        repositories.forEach(repository -> {
            String repositoryName = repository.getRepoName();
            int repoId = repository.getRepoId();
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    "https://api.github.com/repos/{githubName}/{repositoryName}/commits",
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                    githubName,
                    repositoryName
            );
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new GithubRepositoryNotFoundException("Failed to fetch commits for repository: " + repositoryName + " - HTTP " + response.getStatusCode());
            }
            List<Map<String, Object>> responseBody = response.getBody();
            List<Commit> fetchedCommits = (responseBody == null ? Collections.<Map<String, Object>>emptyList() : responseBody)
                    .stream()
                    .map(commitMap -> parseCommit(commitMap, githubName, repositoryName, detailRequest))
                    .toList();

            Optional<GithubCommit> existingCommitOpt = githubCommitRepository.findByUserIdAndRepoId(userId, repoId);
            if (existingCommitOpt.isPresent()) {
                GithubCommit existingCommitDoc = existingCommitOpt.get();
                Set<String> existingCommitShas = existingCommitDoc.getCommits().stream()
                        .map(Commit::getCommitSha)
                        .collect(Collectors.toSet());
                List<Commit> newCommits = fetchedCommits.stream()
                        .filter(commit -> !existingCommitShas.contains(commit.getCommitSha()))
                        .toList();
                if (!newCommits.isEmpty()) {
                    existingCommitDoc.getCommits().addAll(newCommits);
                    githubCommitRepository.save(existingCommitDoc);
                }
            } else {
                GithubCommit newCommitDoc = GithubCommit.builder()
                        .userId(userId)
                        .repoId(repoId)
                        .commits(fetchedCommits)
                        .build();
                githubCommitRepository.save(newCommitDoc);
            }
        });
    }

    /**
     * 1. 메서드 설명: API 응답 데이터에서 commit 정보를 파싱하여 Commit 객체를 생성하는 메서드.
     * 2. 로직:
     *    - commitMap으로부터 commit 정보, SHA, 메시지, 작성자, 작성 날짜를 추출한다.
     *    - fetchFilesChanged()를 호출하여 해당 commit의 파일 변경 내역(코드 파일만)을 조회한다.
     *    - 빌더 패턴을 활용하여 Commit 객체를 생성한다.
     * 3. param:
     *      commitMap      - GitHub commit API 응답 데이터의 Map.
     *      githubName     - GitHub 사용자 이름.
     *      repositoryName - repository 이름.
     *      detailRequest  - 상세 commit API 호출을 위한 HttpEntity.
     * 4. return: 파싱된 정보를 기반으로 생성된 Commit 객체.
     */
    private Commit parseCommit(Map<String, Object> commitMap, String githubName, String repositoryName, HttpEntity<String> detailRequest) {
        @SuppressWarnings("unchecked")
        Map<String, Object> commitInfo = (Map<String, Object>) commitMap.get("commit");
        String sha = (String) commitMap.get("sha");
        String message = (String) commitInfo.get("message");
        @SuppressWarnings("unchecked")
        Map<String, Object> authorInfo = (Map<String, Object>) commitInfo.get("author");
        String dateString = (String) authorInfo.get("date");
        LocalDateTime commitDate = OffsetDateTime.parse(dateString).toLocalDateTime();
        Map<String, String> authorMap = Map.of(
                "name", (String) authorInfo.get("name"),
                "email", (String) authorInfo.get("email")
        );
        List<String> filesChanged = fetchFilesChanged(githubName, repositoryName, sha, detailRequest);
        return Commit.builder()
                .commitSha(sha)
                .commitMessage(message)
                .commitDate(commitDate)
                .author(authorMap)
                .filesChanged(filesChanged)
                .build();
    }

    /**
     * 1. 메서드 설명: GitHub 상세 commit API를 호출하여 commit의 파일 변경 내역에서,
     *    코드 파일(주 언어 파일)에 해당하는 파일의 filename을 추출하여 반환하는 헬퍼 메서드.
     * 2. 로직:
     *    - commit SHA를 이용해 상세 commit API를 호출하고, 응답 상태가 2xx가 아니면 예외를 발생시킨다.
     *    - 응답 데이터의 "files" 항목을 순회하면서, isCodeFile()을 통해 코드 파일로 판단되면 filename을 추출한다.
     *    - 코드 파일이 아닌 경우는 제외하고, 해당 filename들의 List를 반환한다.
     * 3. param:
     *      githubName     - GitHub 사용자 이름.
     *      repositoryName - repository 이름.
     *      sha            - commit SHA.
     *      detailRequest  - 상세 commit API 호출을 위한 HttpEntity.
     * 4. return: 코드 파일에 해당하는 filename들을 포함한 List<String> 객체.
     */
    @SuppressWarnings("unchecked")
    private List<String> fetchFilesChanged(String githubName, String repositoryName, String sha, HttpEntity<String> detailRequest) {
        ResponseEntity<Map<String, Object>> detailResponse = restTemplate.exchange(
                "https://api.github.com/repos/{githubName}/{repositoryName}/commits/{sha}",
                HttpMethod.GET,
                detailRequest,
                new ParameterizedTypeReference<Map<String, Object>>() {},
                githubName,
                repositoryName,
                sha
        );
        if (!detailResponse.getStatusCode().is2xxSuccessful()) {
            throw new GithubRepositoryNotFoundException("Failed to fetch commit details for sha: " + sha + " - HTTP " + detailResponse.getStatusCode());
        }
        Map<String, Object> detailBody = detailResponse.getBody();
        if (detailBody == null) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> filesList = (List<Map<String, Object>>) detailBody.get("files");
        if (filesList == null) {
            return Collections.emptyList();
        }
        return filesList.stream()
                .filter(fileMap -> {
                    String fullFilename = (String) fileMap.get("filename");
                    return isCodeFile(fullFilename);
                })
                .map(fileMap -> (String) fileMap.get("filename"))
                .toList();
    }

    /**
     * 1. 메서드 설명: 파일 이름을 기반으로 해당 파일이 코드 파일(주 언어 파일)인지 판별한다.
     * 2. 로직:
     *    - 허용된 확장자 목록에 해당하면 코드 파일로 판단한다.
     * 3. param:
     *      filename - 파일의 전체 경로 또는 이름.
     * 4. return: 코드 파일이면 true, 그렇지 않으면 false.
     */
    private boolean isCodeFile(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.endsWith(".java") ||
                lower.endsWith(".py") ||
                lower.endsWith(".js") ||
                lower.endsWith(".ts") ||
                lower.endsWith(".html") ||
                lower.endsWith(".css") ||
                lower.endsWith(".cpp") ||
                lower.endsWith(".c") ||
                lower.endsWith(".cs") ||
                lower.endsWith(".rb") ||
                lower.endsWith(".go") ||
                lower.endsWith(".kt");
    }

    // ============================================================
    // 4. Language 관련 메서드
    // ============================================================

    /**
     * 1. 메서드 설명: GitHub API를 호출하여 사용자의 각 repository에 대한 언어 정보를 조회하고,
     *    userId와 repoId를 기준으로 DB에 이미 저장된 GithubLanguage Document가 있으면 업데이트하고,
     *    존재하지 않으면 신규 Document를 삽입하는 메서드.
     * 2. 로직:
     *    - accessToken과 githubName을 사용하여 "https://api.github.com/repos/{githubName}/{repositoryName}/languages" 엔드포인트에서 언어 정보를 조회한다.
     *    - 응답 상태가 2xx가 아니면 예외를 발생시키며, 응답 본문이 null이면 빈 Map으로 처리한다.
     *    - 각 repository에 대해 DB에서 userId와 repoId로 GithubLanguage Document를 조회한다.
     *         * 존재하면, 응답으로 받은 언어 정보를 기존 Document의 languages 필드에 업데이트한 후 저장한다.
     *         * 존재하지 않으면, 새로운 GithubLanguage Document를 생성하여 저장한다.
     * 3. param:
     *      accessToken - GitHub API 접근에 사용되는 access token.
     *      githubName  - GitHub 사용자 이름.
     *      userId      - 현재 애플리케이션 사용자의 로컬 식별자.
     * 4. return: 없음.
     */
    public void saveUserRepositoryLanguage(String accessToken, String githubName, int userId) {
        HttpEntity<String> request = new HttpEntity<>(createHeaders(accessToken, MediaType.APPLICATION_JSON));

        List<Repository> repositories = githubRepoRepository.findByUserId(userId)
                .orElseThrow(() -> new GithubRepositoryNotFoundException("Github repository not found"))
                .getRepositories();

        repositories.forEach(repository -> {
            String repositoryName = repository.getRepoName();
            int repoId = repository.getRepoId();

            ResponseEntity<Map<String, Long>> response = restTemplate.exchange(
                    "https://api.github.com/repos/{githubName}/{repositoryName}/languages",
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<Map<String, Long>>() {},
                    githubName,
                    repositoryName
            );
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new GithubRepositoryNotFoundException("Failed to fetch languages for repository: " + repositoryName
                        + " - HTTP " + response.getStatusCode());
            }
            Map<String, Long> languageMap = response.getBody();
            languageMap = (languageMap == null) ? Collections.emptyMap() : languageMap;

            Optional<GithubLanguage> existingLanguageOpt = githubLanguageRepository.findByUserIdAndRepoId(userId, repoId);
            if (existingLanguageOpt.isPresent()) {
                GithubLanguage existingLanguage = existingLanguageOpt.get();
                existingLanguage.setLanguages(languageMap);
                githubLanguageRepository.save(existingLanguage);
            } else {
                GithubLanguage newLanguage = GithubLanguage.builder()
                        .userId(userId)
                        .repoId(repoId)
                        .languages(languageMap)
                        .build();
                githubLanguageRepository.save(newLanguage);
            }
        });
    }

    // ============================================================
    // 5. Issue 관련 메서드
    // ============================================================

    /**
     * 1. 메서드 설명: GitHub API를 호출하여 사용자의 각 repository에 대한 이슈 정보를 조회하고,
     *    각 repository별로 기존 DB에 저장된 이슈(issueId 기준)와 비교하여 중복되지 않는 신규 이슈만을 DB에 저장하는 메서드.
     * 2. 로직:
     *    - accessToken과 githubName을 사용하여 "https://api.github.com/repos/{userName}/{repositoryName}/issues?state=all" 엔드포인트에서 이슈 정보를 조회한다.
     *    - 응답 상태가 2xx가 아니면 예외를 발생시키며, 응답 본문이 null이면 빈 리스트로 처리한다.
     *    - 조회된 이슈 데이터를 GithubIssue 객체로 매핑한다.
     *    - 각 이슈에 대해 issueId를 기준으로 DB에 이미 존재하는지 확인한다.
     *         * 존재하지 않는 경우에만 신규 이슈 목록에 포함시킨다.
     *    - 신규 이슈가 있을 경우, 이를 DB에 저장한다.
     * 3. param:
     *      accessToken - GitHub API 접근에 사용되는 access token.
     *      githubName  - GitHub 사용자 이름.
     *      userId      - 현재 애플리케이션 사용자의 로컬 식별자.
     * 4. return: 없음.
     */
    public void saveGithubIssues(String accessToken, String githubName, int userId) {
        HttpEntity<String> request = new HttpEntity<>(createHeaders(accessToken, MediaType.valueOf("application/vnd.github.v3+json")));
        List<Repository> repositories = githubRepoRepository.findByUserId(userId)
                .orElseThrow(() -> new GithubRepositoryNotFoundException("Github repository not found"))
                .getRepositories();

        for (Repository repository : repositories) {
            String repositoryName = repository.getRepoName();

            String url = "https://api.github.com/repos/{userName}/{repositoryName}/issues?state=all";
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                    githubName,
                    repositoryName
            );
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new GithubRepositoryNotFoundException("Failed to fetch issues for repository: " + repositoryName + " - HTTP " + response.getStatusCode());
            }
            List<Map<String, Object>> issuesData = Optional.ofNullable(response.getBody())
                    .orElse(Collections.emptyList());

            List<GithubIssue> parsedIssues = issuesData.stream()
                    .filter(Objects::nonNull) // issueMap 자체가 null인 경우 건너뛰기
                    .map(issueMap -> {
                        try {
                            return parseIssue(issueMap);
                        } catch (NullPointerException e) {
                            throw new NullPointerException("Failed to parse issue: " + issueMap.toString());
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();

            List<GithubIssue> newIssues = parsedIssues.stream()
                    .filter(issue -> !githubIssueRepository.existsByIssueId(issue.getIssueId()))
                    .toList();

            if (!newIssues.isEmpty()) {
                githubIssueRepository.saveAll(newIssues);
            }
        }
    }

    /**
     * 1. 메서드 설명: API 응답 데이터의 개별 이슈 정보를 파싱하여 GithubIssue 객체로 변환하는 헬퍼 메서드.
     * 2. 로직:
     *    - 기본 필드(repo_id, issue_id, url, comments_url, title, body, comments)를 추출하고,
     *      중첩 객체(user, labels, assignee, assignees)는 각각 파싱한다.
     * 3. param:
     *      issueMap - GitHub 이슈 API 응답 데이터의 Map.
     * 4. return: 파싱된 정보를 기반으로 생성된 GithubIssue 객체.
     */
    private GithubIssue parseIssue(Map<String, Object> issueMap) {
        if (issueMap == null) {
            throw new IllegalArgumentException("issueMap cannot be null");
        }

        int repoId = Optional.ofNullable(issueMap.get("repo_id"))
                .filter(Number.class::isInstance)
                .map(val -> ((Number) val).intValue())
                .orElse(0);
        long issueId = Optional.ofNullable(issueMap.get("issue_id"))
                .filter(Number.class::isInstance)
                .map(val -> ((Number) val).longValue())
                .orElse(0L);
        String url = Optional.ofNullable(issueMap.get("url"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .orElse("");
        String commentsUrl = Optional.ofNullable(issueMap.get("comments_url"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .orElse("");
        String title = Optional.ofNullable(issueMap.get("title"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .orElse("");
        String body = Optional.ofNullable(issueMap.get("body"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .orElse("");
        int comments = Optional.ofNullable(issueMap.get("comments"))
                .filter(Number.class::isInstance)
                .map(val -> ((Number) val).intValue())
                .orElse(0);

        IssueUser user = null;
        Object userObj = issueMap.get("user");
        if (userObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> userMap = (Map<String, Object>) userObj;
            try {
                user = parseIssueUser(userMap);
            } catch (NullPointerException e) {
                throw new NullPointerException("Failed to parse user: " + userMap);
            }
        }

        List<IssueLabel> labels = Collections.emptyList();
        Object labelsObj = issueMap.get("labels");
        if (labelsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> labelsList = (List<Map<String, Object>>) labelsObj;
            labels = labelsList.stream()
                    .filter(Objects::nonNull)
                    .map(labelMap -> {
                        try {
                            return parseLabel(labelMap);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
        }

        IssueUser assignee = null;
        Object assigneeObj = issueMap.get("assignee");
        if (assigneeObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> assigneeMap = (Map<String, Object>) assigneeObj;
            try {
                assignee = parseIssueUser(assigneeMap);
            } catch (NullPointerException e) {
                throw new NullPointerException("Failed to parse assignee: " + assigneeMap);
            }
        }

        List<IssueUser> assignees = Collections.emptyList();
        Object assigneesObj = issueMap.get("assignees");
        if (assigneesObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> assigneesList = (List<Map<String, Object>>) assigneesObj;
            assignees = assigneesList.stream()
                    .filter(Objects::nonNull)
                    .map(item -> {
                        try {
                            return parseIssueUser(item);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
        }

        return GithubIssue.builder()
                .repoId(repoId)
                .issueId(issueId)
                .url(url)
                .commentsUrl(commentsUrl)
                .title(title)
                .body(body)
                .user(user)
                .labels(labels)
                .assignee(assignee)
                .assignees(assignees)
                .comments(comments)
                .build();
    }

    /**
     * 1. 메서드 설명: GitHub 이슈의 사용자 정보를 파싱하여 GithubIssueUser 객체로 변환하는 헬퍼 메서드.
     * 2. 로직:
     *    - userMap으로부터 login과 id 값을 추출한다.
     * 3. param:
     *      userMap - GitHub 이슈 API 응답의 user 데이터 Map.
     * 4. return: 파싱된 정보를 기반으로 생성된 GithubIssueUser 객체.
     */
    private IssueUser parseIssueUser(Map<String, Object> userMap) {
        String login = (String) userMap.get("login");
        int id = ((Number) userMap.get("id")).intValue();
        return IssueUser.builder()
                .login(login)
                .id(id)
                .build();
    }

    /**
     * 1. 메서드 설명: GitHub 이슈의 레이블 정보를 파싱하여 GithubIssueLabel 객체로 변환하는 헬퍼 메서드.
     * 2. 로직:
     *    - labelMap에서 id, name, color, description 값을 추출한다.
     * 3. param:
     *      labelMap - GitHub 이슈 API 응답의 레이블 데이터 Map.
     * 4. return: 파싱된 정보를 기반으로 생성된 GithubIssueLabel 객체.
     */
    private IssueLabel parseLabel(Map<String, Object> labelMap) {
        int id = ((Number) labelMap.get("id")).intValue();
        String name = (String) labelMap.get("name");
        String color = (String) labelMap.get("color");
        String description = (String) labelMap.get("description");
        return IssueLabel.builder()
                .id(id)
                .name(name)
                .color(color)
                .description(description)
                .build();
    }

    // ============================================================
    // 6. Pull Request 관련 메서드
    // ============================================================

    /**
     * 1. 메서드 설명: GitHub API를 호출하여 사용자의 각 repository에 대한 pull request 정보를 조회하고,
     *    각 repository별로 기존 DB에 저장된 pull request(prId 기준)와 비교하여 중복되지 않는 신규 pull request만을 DB에 저장하는 메서드.
     * 2. 로직:
     *    - accessToken과 githubName을 사용하여 "https://api.github.com/repos/{userName}/{repositoryName}/pulls?state=all" 엔드포인트에서 pull request 정보를 조회한다.
     *    - 응답 상태가 2xx가 아니면 예외를 발생시키며, 응답 본문이 null이면 빈 리스트로 처리한다.
     *    - 조회된 pull request 데이터를 GithubPullRequest 객체로 매핑한다.
     *    - 각 pull request에 대해 prId를 기준으로 DB에 이미 존재하는지 확인한다.
     *         * 존재하지 않는 경우에만 신규 pull request 목록에 포함시킨다.
     *    - 신규 pull request가 있을 경우, 이를 DB에 저장한다.
     * 3. param:
     *      accessToken - GitHub API 접근에 사용되는 access token.
     *      githubName  - GitHub 사용자 이름.
     *      userId      - 현재 애플리케이션 사용자의 로컬 식별자.
     * 4. return: 없음.
     */
    public void saveGithubPullRequests(String accessToken, String githubName, int userId) {
        HttpEntity<String> request = new HttpEntity<>(createHeaders(accessToken, MediaType.valueOf("application/vnd.github.v3+json")));

        List<Repository> repositories = githubRepoRepository.findByUserId(userId)
                .orElseThrow(() -> new GithubRepositoryNotFoundException("Github repository not found"))
                .getRepositories();

        for (Repository repository : repositories) {
            String repositoryName = repository.getRepoName();

            String url = "https://api.github.com/repos/{userName}/{repositoryName}/pulls?state=all";
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                    githubName,
                    repositoryName
            );
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new GithubRepositoryNotFoundException("Failed to fetch pull requests - HTTP " + response.getStatusCode());
            }
            List<Map<String, Object>> prList = Optional.ofNullable(response.getBody())
                    .orElse(Collections.emptyList());

            List<GithubPullRequest> parsedPRs = prList.stream()
                    .map(this::parsePullRequest)
                    .toList();

            List<GithubPullRequest> newPRs = parsedPRs.stream()
                    .filter(pr -> !githubPullRequestRepository.existsByPrId(pr.getPrId()))
                    .toList();

            if (!newPRs.isEmpty()) {
                githubPullRequestRepository.saveAll(newPRs);
            }
        }
    }

    /**
     * 1. 메서드 설명: 주어진 Pull Request 정보를 담은 Map을 파싱하여 GithubPullRequest 객체로 변환한다.
     * 2. 로직:
     *    - prMap이 null이면 IllegalArgumentException을 발생시킨다.
     *    - "base", "repo", "user", "head" 등의 Map 데이터를 추출하여 각각 적절한 값으로 파싱한다.
     *    - 숫자 값은 parseIntValue()를 사용해 파싱하며, 문자열 값은 getStringValue()를 사용한다.
     *    - PR 사용자와 브랜치는 tryParse() 메서드를 통해 안전하게 파싱한다.
     * 3. param:
     *      Map<String, Object> prMap - Pull Request 데이터를 포함한 Map.
     * 4. return: GithubPullRequest 객체.
     */
    private GithubPullRequest parsePullRequest(Map<String, Object> prMap) {
        if (prMap == null) {
            throw new IllegalArgumentException("prMap cannot be null");
        }

        // "base"와 "repo" Map 추출
        Map<String, Object> baseMap = getMap(prMap.get("base"));
        Map<String, Object> baseRepo = getMap(baseMap.get("repo"));

        int repoId = parseIntValue(baseRepo.get("full_name"));
        int prId = parseIntValue(prMap.get("number"));

        // URL, title, body 등 문자열 필드 파싱
        String url = getStringValue(prMap.get("url"));
        String htmlUrl = getStringValue(prMap.get("html_url"));
        String diffUrl = getStringValue(prMap.get("diff_url"));
        String patchUrl = getStringValue(prMap.get("patch_url"));
        String title = getStringValue(prMap.get("title"));
        String body = getStringValue(prMap.get("body"));

        // 댓글 및 커밋 등 카운트 값 파싱
        int commentsCount = parseIntValue(prMap.get("comments"));
        int reviewCommentsCount = parseIntValue(prMap.get("review_comments"));
        int commitsCount = parseIntValue(prMap.get("commits"));

        // 사용자, head, base 브랜치 파싱 (에러 발생 시 로그 기록 후 null 반환)
        Map<String, Object> userMap = getMap(prMap.get("user"));
        PullRequestUser user = tryParse(() -> parsePRUser(userMap), "Error parsing user in pull request");

        Map<String, Object> headMap = getMap(prMap.get("head"));
        PullRequestBranch head = tryParse(() -> parsePRBranch(headMap), "Error parsing head branch in pull request");

        PullRequestBranch base = tryParse(() -> parsePRBranch(baseMap), "Error parsing base branch in pull request");

        return GithubPullRequest.builder()
                .repoId(repoId)
                .prId(prId)
                .url(url)
                .htmlUrl(htmlUrl)
                .diffUrl(diffUrl)
                .patchUrl(patchUrl)
                .title(title)
                .body(body)
                .commentsCount(commentsCount)
                .reviewCommentsCount(reviewCommentsCount)
                .commitsCount(commitsCount)
                .user(user)
                .head(head)
                .base(base)
                .build();
    }

    /**
     * 1. 메서드 설명: 주어진 객체를 Map<String, Object>로 안전하게 캐스팅하여 반환한다.
     *    만약 캐스팅할 수 없으면 빈 Map을 반환한다.
     * 2. 로직:
     *    - 객체가 Map 타입이면 해당 Map으로 캐스팅하고, 그렇지 않으면 Collections.emptyMap()을 반환한다.
     * 3. param:
     *      Object obj - 캐스팅할 대상 객체.
     * 4. return: Map<String, Object> 객체 또는 빈 Map.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Object obj) {
        return (obj instanceof Map) ? (Map<String, Object>) obj : Collections.emptyMap();
    }

    /**
     * 1. 메서드 설명: 주어진 객체를 정수 값으로 파싱한다.
     *    객체가 Number이면 intValue()를 사용하고, String이면 Integer.parseInt()를 사용한다.
     *    파싱에 실패하면 0을 반환한다.
     * 2. 로직:
     *    - 객체 타입에 따라 적절한 파싱 로직을 적용한다.
     * 3. param:
     *      Object value - 파싱할 객체.
     * 4. return: 파싱된 정수 값 또는 실패 시 0.
     */
    private int parseIntValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        } else if (value instanceof String string) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException e) {
                log.warn("Number format exception: {}", string);
                throw new NumberFormatException("Number format exception: " + string);
            }
        }
        return 0;
    }

    /**
     * 1. 메서드 설명: 주어진 객체가 String이면 해당 문자열을 반환하고, 그렇지 않으면 빈 문자열을 반환한다.
     * 2. 로직:
     *    - 객체가 String 타입인지 확인 후 반환한다.
     * 3. param:
     *      Object value - 문자열로 변환할 객체.
     * 4. return: 문자열 또는 빈 문자열.
     */
    private String getStringValue(Object value) {
        return value instanceof String string ? string : "";
    }

    /**
     * 1. 메서드 설명: Supplier를 사용하여 파싱 작업을 수행하며, 예외 발생 시 로그를 기록하고 null을 반환한다.
     * 2. 로직:
     *    - Supplier의 get() 메서드를 호출하여 값을 파싱한다.
     *    - 예외가 발생하면 지정된 에러 메시지와 함께 로그를 남기고 null을 반환한다.
     * 3. param:
     *      Supplier<T> parser - 파싱 작업을 수행하는 람다 함수.
     *      String errorMessage - 에러 발생 시 출력할 메시지.
     * 4. return: 파싱된 값 T 또는 예외 발생 시 null.
     */
    private <T> T tryParse(Supplier<T> parser, String errorMessage) {
        try {
            return parser.get();
        } catch (Exception e) {
            log.error("{}: {}", errorMessage, e.getMessage());
            return null;
        }
    }

    /**
     * 1. 메서드 설명: GitHub Pull Request의 사용자 정보를 파싱하여 GithubPullRequestUser 객체로 변환하는 헬퍼 메서드.
     * 2. 로직:
     *    - userMap으로부터 login과 id 값을 추출하며, id는 int 타입으로 처리한다.
     * 3. param:
     *      userMap - GitHub Pull Request API 응답의 user 데이터 Map.
     * 4. return: 파싱된 정보를 기반으로 생성된 GithubPullRequestUser 객체.
     */
    private PullRequestUser parsePRUser(Map<String, Object> userMap) {
        String login = (String) userMap.get("login");
        int id = ((Number) userMap.get("id")).intValue();
        return PullRequestUser.builder()
                .login(login)
                .id(id)
                .build();
    }

    /**
     * 1. 메서드 설명: Pull Request 응답 데이터의 브랜치(head/base) 정보를 파싱하여 GithubPullRequestBranch 객체로 변환하는 헬퍼 메서드.
     * 2. 로직:
     *    - branchMap으로부터 label, ref, sha 값을 추출하고, 내부의 user 객체는 parsePRUser()를 통해 파싱한다.
     * 3. param:
     *      branchMap - GitHub Pull Request API 응답의 head/base 데이터 Map.
     * 4. return: 파싱된 정보를 기반으로 생성된 GithubPullRequestBranch 객체.
     */
    private PullRequestBranch parsePRBranch(Map<String, Object> branchMap) {
        String label = (String) branchMap.get("label");
        String ref = (String) branchMap.get("ref");
        String sha = (String) branchMap.get("sha");
        PullRequestUser user = null;
        if (branchMap.get("user") != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> userMap = (Map<String, Object>) branchMap.get("user");
            user = parsePRUser(userMap);
        }
        return PullRequestBranch.builder()
                .label(label)
                .ref(ref)
                .sha(sha)
                .user(user)
                .build();
    }

    // ============================================================
    // 7. Event 관련 메서드
    // ============================================================

    /**
     * 1. 메서드 설명: 주어진 GitHub 이벤트를 확인하여 새로운 이벤트가 있는지 또는 최신 이벤트가 90일 이상 오래되었는지 확인한다.
     * 2. 로직:
     *    - GitHub API를 호출하여 사용자의 이벤트 리스트를 가져온다.
     *    - API 응답이 성공적이지 않으면 예외를 발생시킨다.
     *    - 응답에서 가져온 이벤트 중, 주어진 repoNames에 포함된 이벤트만 필터링한다.
     *    - 필터링된 이벤트가 없으면 각 repository에 대해 최신 이벤트가 90일 이전인지 확인한다.
     *    - 필터링된 이벤트가 있으면, 저장된 이벤트와 비교하여 새로운 이벤트를 추출하고,
     *      새로운 이벤트 중 허용된 이벤트 타입(PushEvent, IssuesEvent, PullRequestEvent)이 있는지 검사한다.
     *    - 새로운 이벤트가 없으면 전체 최신 이벤트가 90일 이상 오래되었는지 확인한다.
     * 3. param:
     *      String accessToken - GitHub API 접근 토큰.
     *      String userName - GitHub 사용자 이름.
     *      int userId - 내부 사용자 ID.
     *      List<String> repoNames - 확인할 repository 이름 리스트.
     * 4. return: 새로운 이벤트가 있거나 최신 이벤트가 90일 이상 오래되었으면 true, 그렇지 않으면 false.
     */
    public boolean checkNewGithubEvents(String accessToken, String userName, int userId, List<String> repoNames) {
        HttpEntity<String> request = new HttpEntity<>(createHeaders(accessToken, MediaType.APPLICATION_JSON));
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                "https://api.github.com/users/{userName}/events",
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                userName
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new GithubRepositoryNotFoundException("Failed to fetch events - HTTP " + response.getStatusCode());
        }

        List<Map<String, Object>> apiEvents = response.getBody();
        apiEvents = (apiEvents == null ? Collections.emptyList() : apiEvents);
        Set<String> allowedTypes = Set.of("PushEvent", "IssuesEvent", "PullRequestEvent");

        List<Map<String, Object>> filteredApiEvents = apiEvents.stream()
                .filter(event -> {
                    Map<String, Object> repoMap = getMap(event.get("repo"));
                    String repoName = (String) repoMap.get("name");
                    return repoNames.contains(repoName);
                })
                .toList();

        if (filteredApiEvents.isEmpty()) {
            return shouldNotifyForMissingOrOldEvent(userId, repoNames);
        } else {
            if (processNewEvents(userId, filteredApiEvents, allowedTypes)) {
                return true;
            }
            return isLatestEventOlderThan90Days(userId);
        }
    }

    /**
     * 1. 메서드 설명: 각 repository에 대해 최신 GitHub 이벤트의 생성일이 90일 이전인지 확인한다.
     * 2. 로직:
     *    - 주어진 repository 이름 리스트(repoNames)에 대해, 최신 이벤트를 조회한다.
     *    - 최신 이벤트가 없거나, 최신 이벤트의 생성일이 현재 시간 기준 90일 이전이면 true를 반환한다.
     * 3. param:
     *      int userId - 내부 사용자 ID.
     *      List<String> repoNames - repository 이름 리스트.
     * 4. return: 하나라도 조건을 만족하면 true, 그렇지 않으면 false.
     */
    private boolean shouldNotifyForMissingOrOldEvent(int userId, List<String> repoNames) {
        for (String repoName : repoNames) {
            Optional<GithubEvent> latestEventOpt = githubEventRepository
                    .findTopByUserIdAndEvents_RepoOrderByEventsCreatedAtDesc(userId, repoName);
            if (latestEventOpt.isEmpty() ||
                    latestEventOpt.get().getEvents().getCreatedAt().isBefore(LocalDateTime.now().minusDays(90))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 1. 메서드 설명: 필터링된 API 이벤트 리스트에서 기존에 저장된 이벤트와 비교하여 새로운 이벤트를 추출하고,
     *    새로운 이벤트 중 허용된 이벤트 타입(PushEvent, IssuesEvent, PullRequestEvent)이 존재하는지 검사한다.
     * 2. 로직:
     *    - 사용자(userId)에 대해 저장된 모든 이벤트 ID를 조회한다.
     *    - 필터링된 API 이벤트 중, 저장되지 않은 이벤트만 추출하여 GithubEvent 객체로 변환한다.
     *    - 새로운 이벤트가 존재하면 저장한 후, 허용된 이벤트 타입이 포함되어 있는지 확인하여 결과를 반환한다.
     * 3. param:
     *      int userId - 내부 사용자 ID.
     *      List<Map<String, Object>> filteredApiEvents - 필터링된 API 이벤트 리스트.
     *      Set<String> allowedTypes - 허용된 이벤트 타입 집합.
     * 4. return: 허용된 이벤트 타입을 가진 새로운 이벤트가 있으면 true, 그렇지 않으면 false.
     */
    private boolean processNewEvents(int userId, List<Map<String, Object>> filteredApiEvents, Set<String> allowedTypes) {
        Set<String> existingEventIds = githubEventRepository.findAllByUserId(userId)
                .stream()
                .map(GithubEvent::getGithubEventId)
                .collect(Collectors.toSet());

        List<GithubEvent> newEvents = filteredApiEvents.stream()
                .filter(eventMap -> {
                    String eventId = (String) eventMap.get("id");
                    return !existingEventIds.contains(eventId);
                })
                .map(eventMap -> convertToGithubEvent(eventMap, userId))
                .toList();

        if (!newEvents.isEmpty()) {
            githubEventRepository.saveAll(newEvents);
            return newEvents.stream()
                    .map(githubEvent -> githubEvent.getEvents().getEventType())
                    .anyMatch(allowedTypes::contains);
        }
        return false;
    }

    /**
     * 1. 메서드 설명: GitHub Events API를 호출하여 사용자의 새 repository 생성(CreateEvent) 이벤트가 발생했는지 확인한다.
     *    신규 이벤트가 있을 경우 DB에 저장한 후, 새 repository 생성 이벤트가 감지되면 true를 반환하고,
     *    이벤트가 없거나 최신 이벤트가 90일 이상 오래된 경우에도 true를 반환한다.
     * 2. 로직:
     *    - GitHub API를 호출하여 이벤트 리스트를 가져온다.
     *    - 이벤트 리스트에서 "CreateEvent" 타입 중 payload.ref_type이 "repository"인 이벤트만 필터링한다.
     *    - 필터링된 이벤트가 없으면 최신 이벤트 생성 시간이 90일 이상 오래되었는지 확인하여 결과를 반환한다.
     *    - 필터링된 이벤트가 있으면 DB에 저장된 repository 이름과 비교하여, DB에 없는 새 이벤트가 있으면 저장 후 true를 반환한다.
     *    - 새 이벤트가 없으면 최신 이벤트 생성 시간이 90일 이상 오래되었는지 확인하여 결과를 반환한다.
     * 3. param:
     *      String accessToken - GitHub API 접근에 사용되는 access token.
     *      String userName - GitHub 사용자 이름.
     *      int userId - 로컬 사용자 식별자.
     * 4. return: boolean - 새 repository 생성 이벤트가 감지되었거나 최신 이벤트가 90일 이상 오래되었으면 true, 그렇지 않으면 false.
     */
    public boolean checkNewRepositoryCreationEvents(String accessToken, String userName, int userId) {
        List<Map<String, Object>> apiEvents = fetchApiEvents(accessToken, userName);
        List<Map<String, Object>> createEvents = filterCreateEvents(apiEvents);

        if (createEvents.isEmpty()) {
            return isLatestEventOlderThan90Days(userId);
        }

        Set<String> storedRepoNames = getStoredRepositoryNames(userId);
        List<GithubEvent> newEvents = extractNewEvents(createEvents, userId, storedRepoNames);

        if (!newEvents.isEmpty()) {
            githubEventRepository.saveAll(newEvents);
            return true;
        }

        return isLatestEventOlderThan90Days(userId);
    }

    /**
     * 1. 메서드 설명: GitHub API를 호출하여 사용자의 이벤트 리스트를 가져온다.
     * 2. 로직:
     *    - accessToken과 userName을 이용하여 API 요청을 보내고, 응답 코드가 200번대가 아니면 예외를 발생시킨다.
     *    - 응답 본문이 null이면 빈 리스트를 반환한다.
     * 3. param:
     *      String accessToken - GitHub API 접근 토큰.
     *      String userName - GitHub 사용자 이름.
     * 4. return: List<Map<String, Object>> - GitHub Events API에서 반환된 이벤트 리스트.
     */
    private List<Map<String, Object>> fetchApiEvents(String accessToken, String userName) {
        HttpEntity<String> request = new HttpEntity<>(createHeaders(accessToken, MediaType.APPLICATION_JSON));
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                "https://api.github.com/users/{userName}/events",
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                userName
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new GithubRepositoryNotFoundException("Failed to fetch events - HTTP " + response.getStatusCode());
        }

        return Optional.ofNullable(response.getBody()).orElse(Collections.emptyList());
    }

    /**
     * 1. 메서드 설명: API에서 가져온 이벤트 리스트 중 "CreateEvent" 타입이며, payload.ref_type이 "repository"인 이벤트만 필터링한다.
     * 2. 로직:
     *    - 이벤트의 type이 "CreateEvent"인지 확인하고, payload가 존재하며 ref_type이 "repository"인지 검사한다.
     * 3. param:
     *      List<Map<String, Object>> apiEvents - API에서 받아온 전체 이벤트 리스트.
     * 4. return: List<Map<String, Object>> - 필터링된 repository 생성 이벤트 리스트.
     */
    private List<Map<String, Object>> filterCreateEvents(List<Map<String, Object>> apiEvents) {
        return apiEvents.stream()
                .filter(event -> "CreateEvent".equals(event.get("type")))
                .filter(event -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> payload = (Map<String, Object>) event.get("payload");
                    return payload != null && "repository".equals(payload.get("ref_type"));
                })
                .toList();
    }

    /**
     * 1. 메서드 설명: 사용자 ID를 기반으로 DB에서 해당 사용자의 저장된 repository 이름 목록을 조회한다.
     * 2. 로직:
     *    - GithubRepository를 조회하여, 그 안에 포함된 각 Repository의 이름을 Set으로 반환한다.
     * 3. param:
     *      int userId - 로컬 사용자 식별자.
     * 4. return: Set<String> - DB에 저장된 repository 이름 집합.
     */
    private Set<String> getStoredRepositoryNames(int userId) {
        GithubRepository githubRepository = githubRepoRepository.findByUserId(userId)
                .orElseThrow(() -> new GithubRepositoryNotFoundException("Failed to fetch repository for userId: " + userId));
        return githubRepository.getRepositories().stream()
                .map(Repository::getRepoName)
                .collect(Collectors.toSet());
    }

    /**
     * 1. 메서드 설명: 필터링된 CreateEvent 리스트에서 DB에 저장되지 않은 새 repository 생성 이벤트를 추출한다.
     * 2. 로직:
     *    - 각 이벤트에서 repository 이름을 추출하고, DB에 저장된 이름과 비교하여 새 이벤트만 선택한다.
     *    - 선택된 이벤트를 GithubEvent 객체로 변환하여 리스트로 반환한다.
     * 3. param:
     *      List<Map<String, Object>> createEvents - 필터링된 CreateEvent 이벤트 리스트.
     *      int userId - 로컬 사용자 식별자.
     *      Set<String> storedRepoNames - DB에 저장된 repository 이름 집합.
     * 4. return: List<GithubEvent> - 새로 감지된 GithubEvent 객체 리스트.
     */
    private List<GithubEvent> extractNewEvents(List<Map<String, Object>> createEvents, int userId, Set<String> storedRepoNames) {
        return createEvents.stream()
                .map(event -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> repoMap = (Map<String, Object>) event.get("repo");
                    String repoName = repoMap != null ? (String) repoMap.get("name") : null;
                    return repoName != null ? new AbstractMap.SimpleEntry<>(repoName, event) : null;
                })
                .filter(Objects::nonNull)
                .filter(entry -> !storedRepoNames.contains(entry.getKey()))
                .map(entry -> convertToGithubEvent(entry.getValue(), userId))
                .toList();
    }

    /**
     * 1. 메서드 설명: DB에서 해당 사용자의 최신 이벤트를 조회하고, 최신 이벤트의 생성일이 현재 시간 기준 90일 이전이면 true를 반환하는 메서드.
     * 2. 로직:
     *    - githubEventRepository를 통해 userId에 해당하는 최신 GithubEvent 객체를 조회한다.
     *    - 조회된 이벤트의 생성일(createdAt)이 현재 시간 기준 90일 이전인지 비교한다.
     *    - 이벤트가 없으면 기본값으로 true를 반환한다.
     * 3. param:
     *      int userId - 사용자의 로컬 식별자.
     * 4. return: boolean - 최신 이벤트가 90일 이전이면 true, 아니면 false.
     */
    private boolean isLatestEventOlderThan90Days(int userId) {
        return githubEventRepository.findTopByUserIdOrderByEventsCreatedAtDesc(userId)
                .map(githubEvent -> githubEvent.getEvents().getCreatedAt().isBefore(LocalDateTime.now().minusDays(90)))
                .orElse(true);
    }

    /**
     * 1. 메서드 설명: API 이벤트 데이터(Map<String, Object>)를 GithubEvent 객체로 변환하는 메서드.
     * 2. 로직:
     *    - 이벤트 맵에서 이벤트 ID, 이벤트 타입, repo 객체, 생성 시간 문자열(created_at)을 추출한다.
     *    - repo 객체에서 repository 이름을 추출한다.
     *    - 생성 시간 문자열을 OffsetDateTime을 통해 LocalDateTime으로 변환한다.
     *    - Event 객체와 GithubEvent 객체를 빌더 패턴으로 생성하여 반환한다.
     * 3. param:
     *      Map<String, Object> event - GitHub Events API에서 반환된 이벤트 데이터가 담긴 Map.
     *      int userId - 사용자의 로컬 식별자.
     * 4. return: GithubEvent - 변환된 GithubEvent 객체.
     */
    private GithubEvent convertToGithubEvent(Map<String, Object> event, int userId) {
        String eventId = (String) event.get("id");
        String eventType = (String) event.get("type");
        @SuppressWarnings("unchecked")
        Map<String, Object> repoMap = (Map<String, Object>) event.get("repo");
        String repoName = repoMap != null ? (String) repoMap.get("name") : null;
        String createdAtStr = (String) event.get("created_at");
        LocalDateTime createdAt = OffsetDateTime.parse(createdAtStr).toLocalDateTime();

        Event eventObj = Event.builder()
                .eventType(eventType)
                .repo(repoName)
                .createdAt(createdAt)
                .build();

        return GithubEvent.builder()
                .githubEventId(eventId)
                .userId(userId)
                .events(eventObj)
                .build();
    }

    // ============================================================
    // 8. 공통 헬퍼 메서드
    // ============================================================

    /**
     * 1. 메서드 설명: 주어진 access token과 mediaType을 기반으로 HTTP 헤더를 생성하는 헬퍼 메서드.
     * 2. 로직:
     *    - HttpHeaders 객체를 생성한 후, Bearer 인증 방식으로 access token을 설정하고,
     *      JSON 형식의 응답을 수락하도록 Accept 헤더를 추가한다.
     * 3. param:
     *      accessToken - GitHub API 접근에 사용되는 access token.
     *      mediaType   - 응답으로 수락할 미디어 타입.
     * 4. return: 설정된 HttpHeaders 객체.
     */
    private HttpHeaders createHeaders(String accessToken, MediaType mediaType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Collections.singletonList(mediaType));
        return headers;
    }
}
