package com.gittowork.global.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gittowork.domain.coverletter.entity.CoverLetterAnalysis;
import com.gittowork.domain.fortune.dto.response.GetTodayFortuneResponse;
import com.gittowork.domain.fortune.model.SajuResult;
import com.gittowork.domain.github.entity.GithubAnalysisResult;
import com.gittowork.global.config.GptConfig;
import com.gittowork.global.exception.CoverLetterAnalysisException;
import com.gittowork.global.exception.JsonParsingException;
import com.gittowork.global.properties.OpenAIProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GptService {

    private final GptConfig gptConfig;
    private final RestTemplate restTemplate;
    private final OpenAIProperties openAIProperties;

    private ObjectMapper objectMapper;

    /**
     * 1. 메서드 설명: OpenAI의 ChatGPT API를 호출하여 GitHub 데이터를 분석하는 결과를 JSON 문자열로 받고,
     *    이를 GithubAnalysisResult 객체로 파싱하여 반환하는 메서드.
     * 2. 로직:
     *    - GitHub 분석에 필요한 프롬프트를 generateGithubAnalysisPrompt()를 통해 생성한다.
     *    - 시스템 메시지와 사용자 메시지를 포함하여 GPT API 요청을 구성한 후 callGptApi()를 호출한다.
     *    - GPT API 응답 JSON 문자열을 githubAnalysisResultParser()를 이용해 역직렬화한다.
     * 3. param:
     *      githubAnalysisResult - 분석할 GitHub 데이터가 담긴 객체 (분석 지침 참조).
     *      maxToken - API 호출 시 사용할 최대 토큰 수.
     * 4. return: 분석 결과를 담은 GithubAnalysisResult 객체.
     */
    public GithubAnalysisResult githubDataAnalysis(GithubAnalysisResult githubAnalysisResult, int maxToken) throws JsonProcessingException {
        String prompt = generateGithubAnalysisPrompt(githubAnalysisResult);
        String systemMsg = openAIProperties.getPrompts().getGithubAnalysis().getSystemMsg();
        String responseBody = callGptApi(systemMsg, prompt, maxToken);
        return githubAnalysisResultParser(responseBody);
    }

    /**
     * 1. 메서드 설명: GPT API에 커버레터 분석 요청을 보내고, 응답 JSON을 CoverLetterAnalysis 객체로 파싱하여 반환하는 메서드.
     * 2. 로직:
     *    - 분석에 사용할 커버레터 텍스트와 분석 지침을 포함한 프롬프트를 generateCoverLetterAnalysisPrompt()로 생성한다.
     *    - 시스템 메시지와 사용자 메시지를 포함한 GPT API 요청을 callGptApi()를 통해 전송한다.
     *    - 응답 JSON 문자열을 coverLetterAnalysisResultParser()를 사용해 CoverLetterAnalysis 객체로 역직렬화한다.
     * 3. param:
     *      content - 분석에 사용할 커버레터 텍스트.
     *      maxToken - API 호출 시 사용할 최대 토큰 수.
     * 4. return: 분석 결과를 담은 CoverLetterAnalysis 객체.
     */
    public CoverLetterAnalysis coverLetterAnalysis(String content, int maxToken) {
        String prompt = generateCoverLetterAnalysisPrompt(content);
        String systemMsg = openAIProperties.getPrompts().getCoverLetterAnalysis().getSystemMsg();
        String responseBody = callGptApi(systemMsg, prompt, maxToken);
        return coverLetterAnalysisResultParser(responseBody);
    }

    /**
     * 1. 메서드 설명: OpenAI의 ChatGPT API를 호출하여 사주 명리학 데이터를 기반으로 오늘의 운세를 생성하는 결과를 JSON 문자열로 받고,
     *    이를 GetTodayFortuneResponse 객체로 파싱하여 반환하는 메서드.
     * 2. 로직:
     *    - 사주 명리학에 따른 오늘의 운세 생성을 위해, generateTodayFortunePrompt()를 호출하여 사용자 메시지에 해당하는 프롬프트를 생성한다.
     *    - 제공된 사주 데이터 및 성별, 환경 등의 요소를 반영한 시스템 메시지를 정의한다.
     *    - 시스템 메시지와 프롬프트, 최대 토큰 수(maxToken)를 이용하여 callGptApi()를 호출한다.
     *    - GPT API의 응답(JSON 문자열)을 todayFortuneResultParser()를 통해 파싱하여 GetTodayFortuneResponse 객체로 반환한다.
     * 3. param:
     *      sajuResult - 사주 명리학 계산 결과를 담은 SajuResult 객체.
     *      maxToken - GPT API 호출 시 사용할 최대 토큰 수.
     * 4. return: 오늘의 운세 정보를 담은 GetTodayFortuneResponse 객체.
     */
    public GetTodayFortuneResponse todayFortune(SajuResult sajuResult, int maxToken) {
        String prompt = generateTodayFortunePrompt(sajuResult);
        String systemMsg = openAIProperties.getPrompts().getTodayFortune().getSystemMsg();
        String responseBody = callGptApi(systemMsg, prompt, maxToken);
        return  todayFortuneResultParser(responseBody);
    }

    /**
     * 1. 메서드 설명: 시스템 메시지와 사용자 메시지를 포함한 GPT API 요청을 구성하여 호출하고,
     *    응답 JSON 문자열을 반환하는 공통 메서드.
     * 2. 로직:
     *    - HTTP 헤더에 Content-Type과 Bearer 인증 정보를 설정한다.
     *    - 모델, 온도, 최대 토큰 수 등의 정보를 포함하여 요청 본문(Map<String, Object>)을 구성한다.
     *    - messages 필드에는 시스템 메시지와 사용자 메시지를 순서대로 배열로 포함시킨다.
     *    - restTemplate을 사용하여 GPT API에 POST 요청을 전송하고, 응답 본문을 반환한다.
     * 3. param:
     *      systemMessageContent - 시스템 메시지 내용.
     *      prompt - 사용자 메시지 내용 (분석에 사용할 텍스트와 지침 포함).
     *      maxToken - API 호출 시 사용할 최대 토큰 수.
     * 4. return: GPT API 응답으로 받은 JSON 문자열.
     */
    private String callGptApi(String systemMessageContent, String prompt, int maxToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(gptConfig.getApiKey());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", gptConfig.getModel());

        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemMessageContent);

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        requestBody.put("messages", new Object[]{systemMessage, userMessage});
        requestBody.put("temperature", 0.3);
        requestBody.put("max_tokens", maxToken);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        try {
            String url = openAIProperties.getUrl();
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            String responseBody = response.getBody();
            log.info("GPT API response: {}", responseBody);

            objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode contentNode = rootNode.path("choices").get(0).path("message").path("content");
            String content = contentNode.asText();
            log.info("Extracted GPT Content: {}", content);

            return content;
        } catch (Exception e) {
            throw new CoverLetterAnalysisException("Error calling GPT API");
        }
    }

    /**
     * 1. 메서드 설명: GitHub 분석 결과 객체를 기반으로 GPT API에 보낼 프롬프트를 생성하는 메서드.
     * 2. 로직:
     *    - GitHub 데이터 분석에 필요한 지침과 출력 예시를 포함한 프롬프트 문자열을 구성한다.
     * 3. param:
     *      githubAnalysisResult - 분석에 사용할 GitHub 데이터가 담긴 객체.
     * 4. return: 생성된 프롬프트 문자열.
     */
    private String generateGithubAnalysisPrompt(GithubAnalysisResult githubAnalysisResult) {
        String prompt = openAIProperties.getPrompts().getGithubAnalysis().getUserMsg();
        return String.format(prompt, githubAnalysisResult.toString());
    }

    /**
     * 1. 메서드 설명: 커버레터 분석에 사용할 프롬프트를 생성하는 메서드.
     * 2. 로직:
     *    - 자기소개서 텍스트를 기반으로 분석 지침과 출력 예시를 포함한 프롬프트 문자열을 구성한다.
     * 3. param:
     *      content - 분석에 사용할 커버레터 텍스트.
     * 4. return: 생성된 프롬프트 문자열.
     */
    private String generateCoverLetterAnalysisPrompt(String content) {
        String prompt = openAIProperties.getPrompts().getCoverLetterAnalysis().getUserMsg();
        return String.format(prompt, content);
    }

    /**
     * 1. 메서드 설명: 사주 데이터를 기반으로 GPT API에 보낼 프롬프트를 생성하는 메서드.
     * 2. 로직:
     *    - 오늘의 운세 추출에 필요한 사주 데이터와 출력 예시를 포함한 프롬프트 문자열을 구성한다.
     * 3. param:
     *      SajuResult - 오늘의 운세 추출에 필요한 사주 데이터가 담긴 객체.
     * 4. return: 생성된 프롬프트 문자열.
     */
    private String generateTodayFortunePrompt(SajuResult sajuResult) {
        String prompt = openAIProperties.getPrompts().getTodayFortune().getUserMsg();
        return String.format(prompt, sajuResult.toString());
    }

    /**
     * 1. 메서드 설명: JSON 문자열을 파싱하여 GithubAnalysisResult 객체로 변환하는 메서드.
     * 2. 로직:
     *    - ObjectMapper를 사용하여 입력받은 JSON 문자열을 GithubAnalysisResult 객체로 역직렬화한다.
     * 3. param:
     *      jsonString - 분석 결과를 담은 JSON 문자열.
     * 4. return: 역직렬화된 GithubAnalysisResult 객체.
     */
    private GithubAnalysisResult githubAnalysisResultParser(String jsonString) {
        try {
            return objectMapper.readValue(jsonString, GithubAnalysisResult.class);
        } catch (IOException e) {
            throw new JsonParsingException("GithubAnalysisResult JSON 파싱 중 오류 발생");
        }
    }

    /**
     * 1. 메서드 설명: JSON 문자열을 파싱하여 CoverLetterAnalysis 객체로 변환하는 메서드.
     * 2. 로직:
     *    - ObjectMapper를 사용하여 입력받은 JSON 문자열을 CoverLetterAnalysis 객체로 역직렬화한다.
     * 3. param:
     *      jsonString - 분석 결과를 담은 JSON 문자열.
     * 4. return: 역직렬화된 CoverLetterAnalysis 객체.
     */
    private CoverLetterAnalysis coverLetterAnalysisResultParser(String jsonString) {
        try {
            return objectMapper.readValue(jsonString, CoverLetterAnalysis.class);
        } catch (IOException e) {
            throw new JsonParsingException("CoverLetterAnalysis JSON 파싱 중 오류 발생");
        }
    }

    /**
     * 1. 메서드 설명: JSON 문자열을 파싱하여 GetTodayFortuneResponse 객체로 변환하는 메서드.
     * 2. 로직:
     *    - ObjectMapper를 사용하여 입력받은 JSON 문자열을 GetTodayFortuneResponse 객체로 역직렬화한다.
     * 3. param:
     *      jsonString - 분석 결과를 담은 JSON 문자열.
     * 4. return: 역직렬화된 GetTodayFortuneResponse 객체.
     */
    private GetTodayFortuneResponse todayFortuneResultParser(String jsonString) {
        try {
            return objectMapper.readValue(jsonString, GetTodayFortuneResponse.class);
        } catch (IOException e) {
            throw new JsonParsingException("TodayFortune JSON 파싱 중 오류 발생");
        }
    }
}
