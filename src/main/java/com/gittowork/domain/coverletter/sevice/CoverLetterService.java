package com.gittowork.domain.coverletter.sevice;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.gittowork.domain.coverletter.dto.response.CoverLetterAnalysisStat;
import com.gittowork.domain.coverletter.dto.response.GetCoverLetterAnalysisResponse;
import com.gittowork.domain.coverletter.dto.response.GetMyCoverLetterListResponse.FileInfo;
import com.gittowork.domain.coverletter.dto.response.GetMyCoverLetterListResponse;
import com.gittowork.domain.coverletter.dto.response.UploadCoverLetterResponse;
import com.gittowork.domain.coverletter.entity.CoverLetter;
import com.gittowork.domain.coverletter.entity.CoverLetterAnalysis;
import com.gittowork.domain.coverletter.repository.CoverLetterAnalysisRepository;
import com.gittowork.domain.coverletter.repository.CoverLetterRepository;
import com.gittowork.domain.user.entity.User;
import com.gittowork.domain.user.repository.UserRepository;
import com.gittowork.global.exception.*;
import com.gittowork.global.response.MessageOnlyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoverLetterService {

    private final CoverLetterRepository coverLetterRepository;
    private final CoverLetterAnalysisRepository coverLetterAnalysisRepository;
    private final UserRepository userRepository;
    private final AmazonS3 amazonS3;
    private final CoverLetterAnalysisService coverLetterAnalysisService;

    @Value("${cloud.aws.s3.bucketName}")
    private String bucketName;

    /**
     * 1. 메서드 설명: 전달받은 MultipartFile과 제목 정보를 기반으로 현재 인증된 사용자의 커버레터를 업로드합니다.
     *    업로드된 파일은 Amazon S3에 저장되며, 저장 후 DB에 CoverLetter 엔티티로 기록됩니다.
     *    또한, 비동기로 자기소개서 분석을 수행합니다.
     * 2. 로직:
     *    - SecurityContext에서 현재 인증된 사용자의 username을 조회하여 User 엔티티를 획득합니다.
     *    - 파일 유효성을 검사하고, 파일 확장자 및 Content-Type이 "pdf"인지 검증합니다.
     *    - S3에 파일을 업로드한 후 URL을 획득합니다.
     *    - CoverLetter 엔티티를 DB에 저장하고, 비동기로 coverLetterAnalysis()를 호출합니다.
     * 3. param:
     *      - file: 업로드할 MultipartFile 객체.
     *      - title: 커버레터와 연관된 제목.
     * 4. return: 업로드 결과 메시지와 저장된 CoverLetter의 식별자를 담은 UploadCoverLetterResponse DTO.
     */
    @Transactional
    public UploadCoverLetterResponse uploadCoverLetter(MultipartFile file, String title) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByGithubName(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (file == null || file.isEmpty() || file.getOriginalFilename() == null) {
            throw new EmptyFileException("Empty file input");
        }
        validatePdfFileExtension(file.getOriginalFilename(), file.getContentType());

        String fileUrl = uploadFileToS3(file);

        log.info("File: {}", file.getOriginalFilename());


        CoverLetter coverLetter = coverLetterRepository.save(
                CoverLetter.builder()
                        .user(user)
                        .originName(file.getOriginalFilename())
                        .fileUrl(fileUrl)
                        .createDttm(LocalDateTime.now())
                        .title(title)
                        .build()
        );

        coverLetterAnalysisService.coverLetterAnalysis(file, coverLetter, user);

        return UploadCoverLetterResponse.builder()
                .message("파일 업로드가 성공적으로 완료되었으며, 분석을 시작했습니다.")
                .coverLetterId(coverLetter.getId())
                .build();
    }

    private void validatePdfFileExtension(String filename, String contentType) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            throw new FileExtensionException("File extension not supported");
        }
        String extension = filename.substring(lastDotIndex + 1).toLowerCase();
        if (!"pdf".equals(extension)) {
            throw new FileExtensionException("File extension not supported: " + extension);
        }
        if (!"application/pdf".equalsIgnoreCase(contentType)) {
            throw new FileExtensionException("Content type not supported: " + contentType);
        }
    }

    private String uploadFileToS3(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String fileExtension = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String s3FileName = UUID.randomUUID().toString().substring(0, 10) + fileExtension;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        try {
            PutObjectRequest request = new PutObjectRequest(bucketName, s3FileName, file.getInputStream(), metadata);
            amazonS3.putObject(request);
        } catch (IOException e) {
            throw new S3UploadException("S3 upload failed");
        }
        return amazonS3.getUrl(bucketName, s3FileName).toString();
    }

    /**
     * 1. 메서드 설명: 현재 인증된 사용자의 CoverLetter 목록을 조회하여,
     *    각 CoverLetter 엔티티를 FileInfo DTO로 변환한 후,
     *    이를 포함하는 GetMyCoverLetterListResponse DTO를 반환합니다.
     * 2. 로직:
     *    - SecurityContext에서 현재 인증된 사용자의 username을 조회하고,
     *      이를 기반으로 User 엔티티를 조회합니다.
     *    - User의 ID를 이용해 CoverLetter 목록을 조회한 후, 스트림을 사용해 FileInfo DTO 리스트로 매핑합니다.
     *    - FileInfo 리스트를 포함하는 GetMyCoverLetterListResponse DTO를 반환합니다.
     * 3. param: 없음.
     * 4. return: 사용자의 CoverLetter 정보를 담은 GetMyCoverLetterListResponse DTO.
     */
    @Transactional(readOnly = true)
    public GetMyCoverLetterListResponse getMyCoverLetterList() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByGithubName(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        int userId = user.getId();

        List<FileInfo> fileInfos = coverLetterRepository.findAllByUser_Id(userId)
                .stream()
                .map(cl -> FileInfo.builder()
                        .fileId(cl.getId())
                        .fileName(cl.getOriginName())
                        .fileUrl(cl.getFileUrl())
                        .title(cl.getTitle())
                        .createDttm(cl.getCreateDttm().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        .build())
                .toList();

        return GetMyCoverLetterListResponse.builder()
                .files(fileInfos)
                .build();
    }

    /**
     * 1. 메서드 설명: 전달받은 CoverLetter ID를 기반으로 DB에서 CoverLetter 엔티티를 삭제하고,
     *    해당 CoverLetter에 연결된 파일을 Amazon S3에서 삭제한 후, 삭제 결과 메시지를 반환합니다.
     * 2. 로직:
     *    - coverLetterId로 CoverLetter 엔티티를 조회하고 존재하지 않으면 예외를 발생시킵니다.
     *    - 조회된 CoverLetter 엔티티를 DB에서 삭제한 후, S3에 저장된 파일을 삭제합니다.
     *    - 삭제 완료 메시지를 포함하는 MessageOnlyResponse DTO를 반환합니다.
     * 3. param:
     *      - coverLetterId: 삭제할 CoverLetter의 식별자.
     * 4. return: 삭제 완료 메시지를 담은 MessageOnlyResponse DTO.
     */
    @Transactional
    public MessageOnlyResponse deleteCoverLetter(int coverLetterId) {
        Optional<CoverLetterAnalysis> coverLetterAnalysis = coverLetterAnalysisRepository.findByFile_Id(coverLetterId);

        coverLetterAnalysis.ifPresent(coverLetterAnalysisRepository::delete);

        CoverLetter coverLetter = coverLetterRepository.findById(coverLetterId)
                .orElseThrow(() -> new CoverLetterNotFoundException("CoverLetter not found"));
        coverLetterRepository.delete(coverLetter);

        deleteCoverLetterFromS3(coverLetter.getFileUrl());
        return MessageOnlyResponse.builder()
                .message("파일 삭제 요청 처리 완료")
                .build();
    }

    /**
     * 1. 메서드 설명: 주어진 파일 URL에서 S3 객체 키를 추출한 후,
     *    해당 파일을 Amazon S3에서 삭제합니다.
     * 2. 로직:
     *    - 파일 URL을 파싱하여 경로 정보를 디코딩하고, 선행 슬래시를 제거하여 S3 객체 키를 생성합니다.
     *    - 생성된 키를 사용해 S3에서 파일을 삭제하며, 삭제 실패 시 S3DeleteException을 발생시킵니다.
     * 3. param:
     *      - fileUrl: 삭제할 파일의 URL.
     * 4. return: 없음.
     */
    private void deleteCoverLetterFromS3(String fileUrl) {
        String key = getKeyFromCoverLetterAddress(fileUrl);
        try {
            amazonS3.deleteObject(new DeleteObjectRequest(bucketName, key));
        } catch (Exception e) {
            throw new S3DeleteException("S3 delete failed");
        }
    }

    /**
     * 1. 메서드 설명: 파일 URL에서 S3 객체 키를 추출합니다.
     * 2. 로직:
     *    - URL을 파싱하여 경로 정보를 UTF-8로 디코딩한 후, 선행 슬래시('/')가 있으면 제거한 S3 객체 키를 반환합니다.
     *    - URL 파싱 실패 시 S3DeleteException을 발생시킵니다.
     * 3. param:
     *      - fileUrl: 파일 URL.
     * 4. return: S3 객체 키 (String).
     */
    private String getKeyFromCoverLetterAddress(String fileUrl) {
        try {
            URL url = new URL(fileUrl);
            String decodedPath = URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8);
            return decodedPath.startsWith("/") ? decodedPath.substring(1) : decodedPath;
        } catch (MalformedURLException e) {
            throw new S3DeleteException("S3 delete failed");
        }
    }

    /**
     * 1. 메서드 설명: 전달받은 coverLetterId에 해당하는 CoverLetterAnalysis 엔티티를 조회하고,
     *    해당 엔티티의 분석 정보를 CoverLetterAnalysisStat DTO로 매핑한 후,
     *    파일 URL과 함께 최종 GetCoverLetterAnalysisResponse DTO를 반환하는 읽기 전용 서비스 메서드입니다.
     * 2. 로직:
     *    - coverLetterAnalysisRepository를 통해 coverLetterId에 해당하는 CoverLetterAnalysis 엔티티를 조회합니다.
     *      (존재하지 않을 경우 CoverLetterNotFoundException을 발생시킵니다.)
     *    - 현재 시간을 "yyyy-MM-dd HH:mm:ss" 형식으로 포맷한 후, 이를 analysisDttm 필드에 설정합니다.
     *    - 조회된 CoverLetterAnalysis의 평가 항목들을 CoverLetterAnalysisStat DTO에 매핑하고,
     *      파일 URL도 함께 포함하여 GetCoverLetterAnalysisResponse DTO를 빌더 패턴으로 생성하여 반환합니다.
     * 3. param:
     *      - coverLetterId: 분석 결과를 조회할 CoverLetter의 식별자.
     * 4. return:
     *      - GetCoverLetterAnalysisResponse DTO: CoverLetter의 식별자, 분석 통계, 파일 URL, 분석 일시 정보를 포함합니다.
     */
    @Transactional(readOnly = true)
    public GetCoverLetterAnalysisResponse getCoverLetterAnalysis(int coverLetterId) {
        CoverLetterAnalysis coverLetterAnalysis = coverLetterAnalysisRepository.findByFile_Id(coverLetterId)
                .orElseThrow(() -> new CoverLetterNotFoundException("CoverLetter not found"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        CoverLetterAnalysisStat analysisStat = CoverLetterAnalysisStat.builder()
                .globalCapability(coverLetterAnalysis.getGlobalCapability())
                .challengeSpirit(coverLetterAnalysis.getChallengeSpirit())
                .sincerity(coverLetterAnalysis.getSincerity())
                .communicationSkill(coverLetterAnalysis.getCommunicationSkill())
                .achievementOrientation(coverLetterAnalysis.getAchievementOrientation())
                .responsibility(coverLetterAnalysis.getResponsibility())
                .honesty(coverLetterAnalysis.getHonesty())
                .creativity(coverLetterAnalysis.getCreativity())
                .build();

        return GetCoverLetterAnalysisResponse.builder()
                .coverLetterId(coverLetterId)
                .aiAnalysisResult(coverLetterAnalysis.getAnalysisResult())
                .stat(analysisStat)
                .analysisDttm(LocalDateTime.now().format(formatter))
                .fileUrl(coverLetterAnalysis.getFile().getFileUrl())
                .build();
    }
}
