package com.gittowork.global.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "openai")
@Getter
@Setter
public class OpenAIProperties {
    private Prompts prompts;
    private String url;

    @Getter
    @Setter
    public static class Prompts {
        private GithubAnalysis githubAnalysis;
        private CoverLetterAnalysis coverLetterAnalysis;
        private TodayFortune todayFortune;
    }

    @Getter
    @Setter
    public static class GithubAnalysis {
        private String systemMsg;
        private String userMsg;
    }

    @Getter
    @Setter
    public static class CoverLetterAnalysis {
        private String systemMsg;
        private String userMsg;
    }

    @Getter
    @Setter
    public static class TodayFortune {
        private String systemMsg;
        private String userMsg;
    }
}
