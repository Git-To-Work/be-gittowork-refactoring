package com.gittowork.domain.fortune.service;

import com.gittowork.domain.fortune.dto.request.GetTodayFortuneRequest;
import com.gittowork.domain.fortune.dto.request.InsertFortuneInfoRequest;
import com.gittowork.domain.fortune.dto.response.GetFortuneInfoResponse;
import com.gittowork.domain.fortune.dto.response.GetTodayFortuneResponse;
import com.gittowork.domain.fortune.entity.FortuneInfo;
import com.gittowork.domain.fortune.model.SajuResult;
import com.gittowork.domain.fortune.repository.FortuneInfoRepository;
import com.gittowork.domain.user.entity.User;
import com.gittowork.domain.user.repository.UserRepository;
import com.gittowork.global.exception.fortune.FortuneInfoNotFoundException;
import com.gittowork.global.exception.auth.UserNotFoundException;
import com.gittowork.global.facade.AuthenticationFacade;
import com.gittowork.global.dto.response.MessageOnlyResponse;
import com.gittowork.global.service.openai.GptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 운세 정보를 관리하는 비즈니스 로직을 담당하는 서비스 클래스입니다.
 * <p>
 * - 사용자의 생년월일 및 성별 정보를 저장/조회합니다.
 * - 사주 명리(년·월·일·시)를 계산합니다.
 * - GPT를 통해 오늘의 운세를 생성합니다.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class FortuneService {

    private final FortuneInfoRepository fortuneInfoRepository;
    private final AuthenticationFacade authenticationFacade;
    private final GptService gptService;
    private final SajuCalculator sajuCalculator;
    private final UserRepository userRepository;

    /**
     * 사용자의 운세 정보(생년월일, 시간, 성별)를 저장하거나 업데이트합니다.
     *
     * @param request 사용자가 입력한 운세 정보 요청 DTO
     * @return 저장 완료 메시지를 담은 {@link MessageOnlyResponse}
     * @throws UserNotFoundException 사용자 정보를 찾을 수 없을 때 발생
     */
    @Transactional
    public MessageOnlyResponse insertFortuneInfo(InsertFortuneInfoRequest request) {
        String username = authenticationFacade.getCurrentUsername();
        User user = userRepository.findByGithubName(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        LocalDate birthDt = LocalDate.parse(request.getBirthDt());
        LocalTime birthTm = LocalTime.parse(request.getBirthTm(), DateTimeFormatter.ofPattern("HH:mm"));
        String sex = request.getSex();

        FortuneInfo fortuneInfo = fortuneInfoRepository.findByUser(user)
                .orElseGet(() -> FortuneInfo.builder().user(user).build());
        fortuneInfo.setBirthDt(birthDt);
        fortuneInfo.setTime(birthTm);
        fortuneInfo.setSex(sex);

        fortuneInfoRepository.save(fortuneInfo);

        return MessageOnlyResponse.builder()
                .message("오늘의 운세 사용자 정보가 성공적으로 저장되었습니다.")
                .build();
    }

    /**
     * 현재 사용자의 저장된 운세 정보를 조회합니다.
     *
     * @return 사용자의 운세 정보 응답 DTO {@link GetFortuneInfoResponse}
     * @throws FortuneInfoNotFoundException 운세 정보가 없는 경우 발생
     */
    @Transactional(readOnly = true)
    public GetFortuneInfoResponse getFortuneInfo() {
        String username = authenticationFacade.getCurrentUsername();
        User user = userRepository.findByGithubName(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        FortuneInfo fortuneInfo = fortuneInfoRepository.findByUser(user)
                .orElseThrow(() -> new FortuneInfoNotFoundException("Fortune info not found"));

        return GetFortuneInfoResponse.builder()
                .birthDt(fortuneInfo.getBirthDt())
                .sex(fortuneInfo.getSex())
                .birthTm(fortuneInfo.getTime())
                .build();
    }

    /**
     * 입력된 생년월일과 성별을 기반으로 오늘의 운세를 생성합니다.
     *
     * @param request 오늘의 운세 요청 DTO
     * @return 오늘의 운세 응답 DTO {@link GetTodayFortuneResponse}
     */
    public GetTodayFortuneResponse getTodayFortune(GetTodayFortuneRequest request) {
        LocalDate birthDt = LocalDate.parse(request.getBirthDt());
        LocalTime birthTm = LocalTime.parse(request.getBirthTm());
        LocalDateTime birthDateTime = LocalDateTime.of(birthDt, birthTm);

        // 사주 계산
        SajuResult sajuResult = sajuCalculator.calculateSaju(birthDateTime, request.getSex());
        // GPT 호출로 오늘의 운세 생성
        GetTodayFortuneResponse response = gptService.todayFortune(sajuResult, 1000);

        // 응답에 날짜 설정
        response.getFortune().setDate(LocalDate.now());

        return response;
    }

}
