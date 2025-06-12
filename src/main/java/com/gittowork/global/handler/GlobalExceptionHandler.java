package com.gittowork.global.handler;

import com.gittowork.global.exception.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Getter
    public enum ErrorCode {
        NOT_FOUND("NF","Not found"),
        UNAUTHORIZED("UR", "Unauthorized."),
        DUPLICATE("DP", "Duplicate entry"),
        INTERNAL_SERVER_ERROR("ES", "Internal Server Error."),
        INVALID_ARGUMENT("INA", "Invalid argument"),;

        private final String code;
        private final String message;

        ErrorCode(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }

    @Setter
    @Getter
    @Builder
    public static class ErrorResponse {
        private final String message;
        private final String code;

        public ErrorResponse(String message, String code) {
            this.message = message;
            this.code = code;
        }
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String code, String message) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(message)
                .code(code)
                .build();
        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(UserNotFoundException e) {
        log.warn("User not found: {}", e.getMessage());
        String message = e.getMessage() == null ? ErrorCode.NOT_FOUND.getMessage() : e.getMessage();
        return buildErrorResponse(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND.getCode(), message);
    }

    @ExceptionHandler(GithubSignInException.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(GithubSignInException e) {
        log.warn("Github sign in: {}", e.getMessage());
        String message = e.getMessage() == null ? ErrorCode.UNAUTHORIZED.getMessage() : e.getMessage();
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getCode(), message);
    }

    @ExceptionHandler(AutoLogInException.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(AutoLogInException e) {
        log.warn("Auto log in: {}", e.getMessage());
        String message = e.getMessage() == null ? ErrorCode.UNAUTHORIZED.getMessage() : e.getMessage();
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getCode(), message);
    }

    @ExceptionHandler(AccessTokenNotFoundException.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(AccessTokenNotFoundException e) {
        log.warn("Access token not found: {}", e.getMessage());
        String message = e.getMessage() == null ? ErrorCode.UNAUTHORIZED.getMessage() : e.getMessage();
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getCode(), message);
    }

    @ExceptionHandler(DataNotFoundException.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(DataNotFoundException e) {
        log.warn("Data not found: {}", e.getMessage());
        String message = e.getMessage() == null ? ErrorCode.NOT_FOUND.getMessage() : e.getMessage();
        return buildErrorResponse(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND.getCode(), message);
    }

    @ExceptionHandler(CompanyNotFoundException.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(CompanyNotFoundException e) {
        log.warn("Company Not Found: {}", e.getMessage());
        String message = e.getMessage() == null ? ErrorCode.NOT_FOUND.getMessage() : e.getMessage();
        return buildErrorResponse(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND.getCode(), message);
    }

    @ExceptionHandler(UserInteractionNotFoundException.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(UserInteractionNotFoundException e) {
        log.warn("User Interaction Not Found: {}", e.getMessage());
        String message = e.getMessage() == null ? ErrorCode.NOT_FOUND.getMessage() : e.getMessage();
        return buildErrorResponse(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND.getCode(), message);
    }

    @ExceptionHandler(InteractionDuplicateException.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(InteractionDuplicateException e) {
        log.warn("Interaction Duplicate Exception: {}", e.getMessage());
        String message = e.getMessage() == null ? ErrorCode.DUPLICATE.getMessage() : e.getMessage();
        return buildErrorResponse(HttpStatus.CONFLICT, ErrorCode.DUPLICATE.getCode(), message);
    }

    @ExceptionHandler(GithubRepositoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(GithubRepositoryNotFoundException e) {
        log.warn("Github repository not found: {}", e.getMessage());
        String message = e.getMessage() == null ? ErrorCode.NOT_FOUND.getMessage() : e.getMessage();
        return buildErrorResponse(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND.getCode(), message);
    }

    @ExceptionHandler(GithubCommitsNotFoundException.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(GithubCommitsNotFoundException e) {
        log.warn("Github commits not found: {}", e.getMessage());
        String message = e.getMessage() == null ? ErrorCode.NOT_FOUND.getMessage() : e.getMessage();
        return buildErrorResponse(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND.getCode(), message);
    }

    @ExceptionHandler(SonarAnalysisException.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(SonarAnalysisException e) {
        log.warn("Sonar Analysis failed: {}", e.getMessage());
        String message = e.getMessage() == null ? ErrorCode.INTERNAL_SERVER_ERROR.getMessage() : e.getMessage();
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.getCode(), message);
    }

    @ExceptionHandler(GithubAnalysisException.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(GithubAnalysisException e) {
        log.warn("Github analysis failed: {}", e.getMessage());
        String message = e.getMessage() == null ? ErrorCode.INTERNAL_SERVER_ERROR.getMessage() : e.getMessage();
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.getCode(), message);
    }

    @ExceptionHandler(GithubAnalysisNotFoundException.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(GithubAnalysisNotFoundException e) {
        log.warn("Github analysis not found: {}", e.getMessage());
        String message = e.getMessage() == null ? ErrorCode.NOT_FOUND.getMessage() : e.getMessage();
        return buildErrorResponse(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND.getCode(), message);
    }

    @ExceptionHandler(EmptyFileException.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(EmptyFileException e) {
        log.warn("Empty file: {}", e.getMessage());
        String message = e.getMessage() == null ? ErrorCode.INVALID_ARGUMENT.getMessage() : e.getMessage();
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_ARGUMENT.getCode(), message);
    }

    @ExceptionHandler(S3UploadException.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(S3UploadException e) {
        log.warn("S3 upload failed: {}", e.getMessage());
        String message = e.getMessage() == null ? ErrorCode.INTERNAL_SERVER_ERROR.getMessage() : e.getMessage();
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.getCode(), message);
    }

    @ExceptionHandler(FileExtensionException.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(FileExtensionException e) {
        log.warn("File extension not supported: {}", e.getMessage());
        String message = e.getMessage() == null ? ErrorCode.INVALID_ARGUMENT.getMessage() : e.getMessage();
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_ARGUMENT.getCode(), message);
    }

    @ExceptionHandler(CoverLetterNotFoundException.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(CoverLetterNotFoundException e) {
        log.warn("Cover letter not found: {}", e.getMessage());
        String message = e.getMessage() == null ? ErrorCode.NOT_FOUND.getMessage() : e.getMessage();
        return buildErrorResponse(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND.getCode(), message);
    }

    @ExceptionHandler(S3DeleteException.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(S3DeleteException e) {
        log.warn("S3 delete failed: {}", e.getMessage());
        String message = e.getMessage() == null ? ErrorCode.INTERNAL_SERVER_ERROR.getMessage() : e.getMessage();
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.getCode(), message);
    }

    @ExceptionHandler(CoverLetterAnalysisNotFoundException.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(CoverLetterAnalysisNotFoundException e) {
        log.warn("Cover letter analysis not found: {}", e.getMessage());
        String message = e.getMessage() == null ? ErrorCode.NOT_FOUND.getMessage() : e.getMessage();
        return buildErrorResponse(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND.getCode(), message);
    }

    @ExceptionHandler(CoverLetterAnalysisException.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(CoverLetterAnalysisException e) {
        log.warn("Cover letter analysis not found: {}", e.getMessage());
        String message = e.getMessage() == null ? ErrorCode.INTERNAL_SERVER_ERROR.getMessage() : e.getMessage();
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.getCode(), message);
    }

    @ExceptionHandler(JsonParsingException.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(JsonParsingException e) {
        log.warn("Json parsing failed: {}", e.getMessage());
        String message = e.getMessage() == null ? ErrorCode.INTERNAL_SERVER_ERROR.getMessage() : e.getMessage();
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.getCode(), message);
    }

    @ExceptionHandler(SelectedRepositoryDuplicatedException.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(SelectedRepositoryDuplicatedException e) {
        log.warn("Selected repository duplicated: {}", e.getMessage());
        String message = e.getMessage() == null ? ErrorCode.DUPLICATE.getMessage() : e.getMessage();
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ErrorCode.DUPLICATE.getCode(), message);
    }

    @ExceptionHandler(WrongQuizTypeException.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(WrongQuizTypeException e) {
        log.warn("Wrong quiz type: {}", e.getMessage());
        String message = e.getMessage() == null ? ErrorCode.INVALID_ARGUMENT.getMessage() : e.getMessage();
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_ARGUMENT.getCode(), message);
    }

    @ExceptionHandler(FirebaseMessageException.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(FirebaseMessageException e) {
        log.warn("FirebaseMessageException: {}", e.getMessage());
        String message = e.getMessage() == null ? ErrorCode.INTERNAL_SERVER_ERROR.getMessage() : e.getMessage();
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.getCode(), message);
    }

    @ExceptionHandler(FortuneInfoNotFoundException.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(FortuneInfoNotFoundException e) {
        log.warn("Fortune info not found: {}", e.getMessage());
        String message = e.getMessage() == null ? ErrorCode.NOT_FOUND.getMessage() : e.getMessage();
        return buildErrorResponse(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND.getCode(), message);
    }
}
