package com.gittowork.domain.coverletter.sevice;

import com.gittowork.domain.coverletter.dto.response.*;
import com.gittowork.domain.coverletter.entity.CoverLetter;
import com.gittowork.domain.coverletter.entity.CoverLetterAnalysis;
import com.gittowork.domain.coverletter.repository.CoverLetterAnalysisRepository;
import com.gittowork.domain.coverletter.repository.CoverLetterRepository;
import com.gittowork.domain.user.entity.User;
import com.gittowork.domain.user.repository.UserRepository;
import com.gittowork.global.exception.*;
import com.gittowork.global.facade.AuthenticationFacade;
import com.gittowork.global.response.MessageOnlyResponse;
import com.gittowork.global.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CoverLetter(자기소개서) 관련 주요 비즈니스 로직을 처리하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CoverLetterService {

    private final CoverLetterRepository coverLetterRepository;
    private final CoverLetterAnalysisRepository coverLetterAnalysisRepository;
    private final UserRepository userRepository;
    private final CoverLetterAnalysisService coverLetterAnalysisService;
    private final S3Service s3Service;
    private final AuthenticationFacade authenticationFacade;

    /**
     * 사용자가 업로드한 PDF 형식의 자기소개서를 S3에 저장하고, 분석을 비동기로 시작합니다.
     *
     * @param file  업로드된 PDF 파일
     * @param title 파일에 지정할 제목
     * @return 업로드 결과 메시지 및 생성된 CoverLetter ID
     * @throws EmptyFileException         파일이 비어 있거나 유효하지 않은 경우
     * @throws FileExtensionException     PDF 확장자나 Content-Type이 올바르지 않은 경우
     * @throws UserNotFoundException      현재 인증된 사용자를 찾을 수 없는 경우
     * @throws S3UploadException          S3 업로드에 실패한 경우
     */
    public UploadCoverLetterResponse uploadCoverLetter(MultipartFile file, String title) {
        String username = authenticationFacade.getCurrentUsername();
        User user = userRepository.findByGithubName(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (file == null || file.isEmpty() || file.getOriginalFilename() == null) {
            throw new EmptyFileException("Empty file input");
        }
        validatePdfFileExtension(file.getOriginalFilename(), file.getContentType());

        String fileUrl = s3Service.uploadFileToS3(file);

        CoverLetter coverLetter = coverLetterRepository.save(
                CoverLetter.builder()
                        .user(user)
                        .originName(file.getOriginalFilename())
                        .fileUrl(fileUrl)
                        .title(title)
                        .build()
        );

        coverLetterAnalysisService.coverLetterAnalysis(file, coverLetter, user);

        return UploadCoverLetterResponse.builder()
                .message("파일 업로드가 성공적으로 완료되었으며, 분석을 시작했습니다.")
                .coverLetterId(coverLetter.getId())
                .build();
    }

    /**
     * 현재 인증된 사용자가 업로드한, 삭제되지 않은 자기소개서 목록을 조회합니다.
     *
     * @return 사용자의 CoverLetter 목록을 담은 DTO
     * @throws UserNotFoundException 인증된 사용자를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public GetMyCoverLetterListResponse getMyCoverLetterList() {
        String username = authenticationFacade.getCurrentUsername();
        User user = userRepository.findByGithubName(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        int userId = user.getId();

        List<FileInfo> fileInfos = coverLetterRepository
                .findAllByUser_IdAndDeletedDttmIsNull(userId)
                .stream()
                .map(cl -> FileInfo.builder()
                        .fileId(cl.getId())
                        .fileName(cl.getOriginName())
                        .fileUrl(cl.getFileUrl())
                        .title(cl.getTitle())
                        .createDttm(cl.getCreatedDttm())
                        .build())
                .collect(Collectors.toList());

        return GetMyCoverLetterListResponse.builder()
                .files(fileInfos)
                .build();
    }

    /**
     * 지정된 CoverLetter와 그에 연관된 분석 결과를 soft-delete 처리한 후,
     * S3에 저장된 파일을 실제로 삭제합니다.
     *
     * @param coverLetterId 삭제할 CoverLetter의 ID
     * @return 삭제 처리 완료 메시지
     * @throws CoverLetterNotFoundException        해당 ID의 CoverLetter를 찾을 수 없는 경우
     * @throws S3DeleteException                   S3에서 파일 삭제에 실패한 경우
     */
    public MessageOnlyResponse deleteCoverLetter(Integer coverLetterId) {
        LocalDateTime now = LocalDateTime.now();

        // 분석 결과 soft-delete
        coverLetterAnalysisRepository.findByFile_Id(coverLetterId)
                .ifPresent(a -> a.setDeletedDttm(now));

        // CoverLetter soft-delete
        CoverLetter coverLetter = coverLetterRepository.findById(coverLetterId)
                .orElseThrow(() -> new CoverLetterNotFoundException("CoverLetter not found"));
        coverLetter.setDeletedDttm(now);

        // S3 물리 삭제
        s3Service.deleteCoverLetterFromS3(coverLetter.getFileUrl());

        return MessageOnlyResponse.builder()
                .message("파일 삭제 요청 처리 완료")
                .build();
    }

    /**
     * 지정된 CoverLetter에 대해 AI 분석 결과를 조회하여 반환합니다.
     *
     * @param coverLetterId 분석 결과를 조회할 CoverLetter의 ID
     * @return 분석 결과와 각 항목의 통계 정보를 담은 DTO
     * @throws CoverLetterNotFoundException            해당 ID의 분석 데이터를 찾을 수 없는 경우
     * @throws CoverLetterAnalysisAccessDenyException  분석 데이터에 대한 접근 권한이 없는 경우
     */
    @Transactional(readOnly = true)
    public GetCoverLetterAnalysisResponse getCoverLetterAnalysis(Integer coverLetterId) {
        CoverLetterAnalysis coverLetterAnalysis = coverLetterAnalysisRepository.findByFile_Id(coverLetterId)
                .orElseThrow(() -> new CoverLetterNotFoundException("CoverLetter not found"));

        String username = authenticationFacade.getCurrentUsername();
        if (!coverLetterAnalysis.getUser().getGithubName().equals(username)) {
            throw new CoverLetterAnalysisAccessDenyException("Cover letter analysis access denied");
        }

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
                .fileUrl(coverLetterAnalysis.getFile().getFileUrl())
                .build();
    }

    /**
     * 파일명과 Content-Type을 검사하여 PDF 확장자인지 검증합니다.
     *
     * @param filename    검사할 파일명
     * @param contentType 검사할 Content-Type
     * @throws FileExtensionException 파일명이 없거나, 확장자/Content-Type이 PDF가 아닌 경우
     */
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
}
