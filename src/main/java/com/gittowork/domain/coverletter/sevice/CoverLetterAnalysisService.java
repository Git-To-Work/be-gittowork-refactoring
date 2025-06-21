package com.gittowork.domain.coverletter.sevice;

import com.gittowork.domain.coverletter.entity.CoverLetter;
import com.gittowork.domain.coverletter.entity.CoverLetterAnalysis;
import com.gittowork.domain.coverletter.repository.CoverLetterAnalysisRepository;
import com.gittowork.domain.firebase.service.FirebaseService;
import com.gittowork.domain.user.entity.User;
import com.gittowork.global.exception.file.EmptyFileException;
import com.gittowork.global.exception.firebase.FirebaseMessageException;
import com.gittowork.global.service.openai.GptService;
import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 업로드된 자기소개서를 비동기로 분석하고,
 * 분석 결과를 저장한 뒤 Firebase를 통해 알림을 전송하는 서비스 클래스입니다.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CoverLetterAnalysisService {

    private final CoverLetterAnalysisRepository coverLetterAnalysisRepository;
    private final GptService gptService;
    private final FirebaseService firebaseService;

    /**
     * 자기소개서(PDF) 파일을 분석하는 비동기 메서드입니다.
     * <ol>
     *   <li>PDF 파일의 텍스트를 추출합니다.</li>
     *   <li>추출된 텍스트로 GPT 기반 분석을 수행합니다.</li>
     *   <li>분석 결과를 새로운 트랜잭션으로 저장합니다.</li>
     *   <li>분석 완료 알림을 Firebase로 전송합니다.</li>
     * </ol>
     *
     * @param file          분석할 PDF 파일
     * @param coverLetter   분석 대상 CoverLetter 엔티티
     * @param user          파일을 업로드한 사용자 정보
     * @throws EmptyFileException        파일이 비어 있거나 파싱에 실패한 경우 발생
     * @throws FirebaseMessageException  알림 전송에 실패한 경우 발생
     */
    @Async("analysisExecutor")
    public void coverLetterAnalysis(MultipartFile file, CoverLetter coverLetter, User user) {
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null) {
            throw new EmptyFileException("파일이 비어 있습니다.");
        }

        String content = extractText(file);
        CoverLetterAnalysis coverLetterAnalysis = analyze(content);
        saveAnalysis(coverLetterAnalysis, coverLetter, user);
        sendAnalysisComplete(user);
    }

    /**
     * 분석 결과를 별도의 트랜잭션(REQUIRES_NEW)으로 저장합니다.
     *
     * @param analysis      저장할 분석 결과 엔티티
     * @param coverLetter   분석 대상 CoverLetter 엔티티
     * @param user          분석을 실행한 사용자 정보
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAnalysis(CoverLetterAnalysis analysis, CoverLetter coverLetter, User user) {
        analysis.setFile(coverLetter);
        analysis.setUser(user);
        coverLetterAnalysisRepository.save(analysis);
    }

    /**
     * PDF 파일에서 텍스트를 추출합니다.
     *
     * @param file  텍스트를 추출할 PDF 파일
     * @return 추출된 텍스트 문자열
     * @throws EmptyFileException  I/O 오류로 파싱에 실패한 경우 발생
     */
    private String extractText(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             PDDocument doc = PDDocument.load(is)) {
            return new PDFTextStripper().getText(doc);
        } catch (IOException e) {
            throw new EmptyFileException("PDF 텍스트 추출에 실패했습니다.");
        }
    }

    /**
     * 추출된 텍스트를 기반으로 GPT 분석을 수행합니다.
     *
     * @param content 분석할 텍스트 내용
     * @return 분석 결과를 담은 CoverLetterAnalysis 엔티티
     */
    private CoverLetterAnalysis analyze(String content) {
        return gptService.coverLetterAnalysis(content, 500);
    }

    /**
     * 분석 완료 후 Firebase 푸시 알림을 전송합니다.
     *
     * @param user  알림을 받을 사용자 정보
     * @throws FirebaseMessageException  알림 전송 실패 시 발생
     */
    private void sendAnalysisComplete(User user) {
        try {
            firebaseService.sendCoverLetterMessage(
                    user,
                    "자기소개서 분석 완료",
                    user.getGithubName() + "님, 자기소개서 분석이 완료되었습니다.",
                    "CoverLetterAnalysis"
            );
        } catch (FirebaseMessagingException e) {
            throw new FirebaseMessageException("푸시 알림 전송에 실패했습니다.");
        }
    }
}
