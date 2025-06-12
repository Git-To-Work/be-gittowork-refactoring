package com.gittowork.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @io.swagger.v3.oas.annotations.info.Info(title = "API Documentation of gittowork", version = "0.0"))
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        Info info = new Info()
                .title("gittowork")
                .description("<h3>C103 OneForAll</h3>")
                .version("v1");

        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .info(info);
    }

    @Bean
    public GroupedOpenApi versionApi() {
        return GroupedOpenApi.builder()
                .group("API Versions")
                .pathsToMatch("/api/**")
                .build();
    }


    @Bean
    public GroupedOpenApi githubAuthenticationApi() {
        return GroupedOpenApi.builder()
                .group("Github Authentication")
                .pathsToMatch("/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("User API")
                .pathsToMatch("/user/**")
                .build();
    }

    @Bean
    public GroupedOpenApi githubApi() {
        return GroupedOpenApi.builder()
                .group("Github API")
                .pathsToMatch("/github/**")
                .build();
    }

    @Bean
    public GroupedOpenApi coverLetterApi() {
        return GroupedOpenApi.builder()
                .group("CoverLetter API")
                .pathsToMatch("/cover-letter/**")
                .build();
    }

    @Bean
    public GroupedOpenApi userInteractionApi() {
        return GroupedOpenApi.builder()
                .group("userInteraction API")
                .pathsToMatch("/company-interaction/**")
                .build();
    }

    @Bean
    public GroupedOpenApi quizApi() {
        return GroupedOpenApi.builder()
                .group("developerQuiz API")
                .pathsToMatch("/quiz/**")
                .build();
    }

    @Bean
    public GroupedOpenApi firebaseApi() {
        return GroupedOpenApi.builder()
                .group("firebase API")
                .pathsToMatch("/firebase/**")
                .build();
    }

    @Bean
    public GroupedOpenApi fortuneAPI() {
        return GroupedOpenApi.builder()
                .group("fortune API")
                .pathsToMatch("/fortune/**")
                .build();
    }
}
