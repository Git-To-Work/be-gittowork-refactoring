package com.gittowork.domain.fortune.service;

import com.gittowork.domain.fortune.dto.request.GetTodayFortuneRequest;
import com.gittowork.domain.fortune.dto.request.InsertFortuneInfoRequest;
import com.gittowork.domain.fortune.dto.response.GetFortuneInfoResponse;
import com.gittowork.domain.fortune.dto.response.GetTodayFortuneResponse;
import com.gittowork.domain.fortune.entity.FortuneInfo;
import com.gittowork.domain.fortune.model.SajuResult;
import com.gittowork.domain.fortune.model.SolarTerm;
import com.gittowork.domain.fortune.repository.FortuneInfoRepository;

import com.gittowork.domain.user.entity.User;
import com.gittowork.domain.user.repository.UserRepository;
import com.gittowork.global.exception.FortuneInfoNotFoundException;
import com.gittowork.global.exception.UserNotFoundException;
import com.gittowork.global.response.MessageOnlyResponse;
import com.gittowork.global.service.GptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FortuneService {

    private final FortuneInfoRepository fortuneInfoRepository;
    private final UserRepository userRepository;
    private final GptService gptService;

    private static final String[] HEAVENLY_STEMS = {"갑", "을", "병", "정", "무", "기", "경", "신", "임", "계"};
    private static final String[] EARTHLY_BRANCHES = {"자", "축", "인", "묘", "진", "사", "오", "미", "신", "유", "술", "해"};

    private static final LocalDate BASE_DATE = LocalDate.of(1900, 1, 31);

    private static final List<SolarTerm> SOLAR_TERMS = Arrays.asList(
            new SolarTerm(1, 5, "축"),
            new SolarTerm(2, 4, "인"),
            new SolarTerm(3, 6, "묘"),
            new SolarTerm(4, 5, "진"),
            new SolarTerm(5, 6, "사"),
            new SolarTerm(6, 5, "오"),
            new SolarTerm(7, 7, "미"),
            new SolarTerm(8, 8, "신"),
            new SolarTerm(9, 8, "유"),
            new SolarTerm(10, 8, "술"),
            new SolarTerm(11, 7, "해"),
            new SolarTerm(12, 7, "자")
    );

    /**
     * 1. 메서드 설명: 현재 인증된 사용자에 대한 운세 정보를 저장하거나 업데이트하는 메서드이다.
     *    입력된 생년월일, 태어난 시간, 성별 정보를 사용하여 FortuneInfo 엔티티를 생성 또는 갱신한 후, DB에 저장한다.
     * 2. 로직:
     *    - SecurityContext에서 현재 인증된 사용자를 조회한다.
     *    - InsertFortuneInfoRequest에서 생년월일, 태어난 시간, 성별 정보를 파싱한다.
     *    - FortuneInfoRepository를 통해 해당 사용자의 기존 운세 정보를 조회한다.
     *      - 기존 정보가 있으면 해당 엔티티를 업데이트한다.
     *      - 없으면 새로운 FortuneInfo 엔티티를 생성한다.
     *    - 업데이트 또는 생성된 FortuneInfo 엔티티를 DB에 저장한다.
     * 3. param:
     *      insertFortuneInfoRequest - 생년월일, 태어난 시간, 성별 정보를 포함하는 요청 DTO.
     * 4. return:
     *      MessageOnlyResponse - 저장 성공 메시지를 포함하는 응답 객체.
     */
    @Transactional
    public MessageOnlyResponse insertFortuneInfo(InsertFortuneInfoRequest insertFortuneInfoRequest) {
        User user = getUser();

        LocalDate birthDt = LocalDate.parse(insertFortuneInfoRequest.getBirthDt());
        LocalTime birthTm = LocalTime.parse(insertFortuneInfoRequest.getBirthTm(), DateTimeFormatter.ofPattern("HH:mm"));
        String sex = insertFortuneInfoRequest.getSex();

        FortuneInfo fortuneInfo = fortuneInfoRepository.findByUser(user)
                .orElseGet(() -> FortuneInfo.builder().user(user).build());
        fortuneInfo.setBirthDt(birthDt);
        fortuneInfo.setTime(birthTm);
        fortuneInfo.setSex(sex);

        fortuneInfoRepository.save(fortuneInfo);

        return MessageOnlyResponse.builder()
                .message("오늘의 운세 사용자 정보가 성공적으로 저장되었슴니다.")
                .build();
    }

    /**
     * 1. 메서드 설명: 현재 인증된 사용자의 운세 정보를 조회하여 반환하는 메서드.
     * 2. 로직:
     *    - 현재 인증된 사용자를 SecurityContext에서 조회한다.
     *    - 해당 사용자의 FortuneInfo 데이터를 FortuneInfoRepository를 통해 조회하며, 없으면 예외 발생.
     *    - 조회한 데이터를 GetFortuneInfoResponse DTO로 변환하여 반환한다.
     * 3. param: 없음
     * 4. return: GetFortuneInfoResponse (사용자의 운세 정보)
     */
    @Transactional(readOnly = true)
    public GetFortuneInfoResponse getFortuneInfo() {
        User user = getUser();

        FortuneInfo fortuneInfo = fortuneInfoRepository.findByUser(user)
                .orElseThrow(() -> new FortuneInfoNotFoundException("Fortune info not found"));

        return GetFortuneInfoResponse.builder()
                .birthDt(fortuneInfo.getBirthDt().toString())
                .sex(fortuneInfo.getSex())
                .birthTm(fortuneInfo.getTime().toString())
                .build();
    }

    /**
     * 1. 메서드 설명: 오늘의 운세 생성을 위해 입력된 생년월일, 태어난 시간을 바탕으로 사주를 계산하고,
     *    GPT 서비스를 호출하여 오늘의 운세를 받아오는 메서드.
     * 2. 로직:
     *    - 입력된 생년월일과 태어난 시간을 파싱하여 LocalDateTime 객체로 생성한다.
     *    - calculateSaju()를 호출하여 SajuResult(사주의 구성)을 계산한다.
     *    - gptService.todayFortune()를 호출하여 오늘의 운세를 생성한 후 반환한다.
     * 3. param: GetTodayFortuneRequest (생년월일, 태어난 시간, 성별 정보를 포함)
     * 4. return: GetTodayFortuneResponse (오늘의 운세 정보를 포함)
     */
    public GetTodayFortuneResponse getTodayFortune(GetTodayFortuneRequest getTodayFortuneRequest) {
        LocalDate birthDt = LocalDate.parse(getTodayFortuneRequest.getBirthDt());
        LocalTime birthTm = LocalTime.parse(getTodayFortuneRequest.getBirthTm());
        LocalDateTime birthDateTime = LocalDateTime.of(birthDt, birthTm);

        SajuResult sajuResult = calculateSaju(birthDateTime, getTodayFortuneRequest.getSex());
        GetTodayFortuneResponse getTodayFortuneResponse = gptService.todayFortune(sajuResult, 1000);

        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        GetTodayFortuneResponse.Fortune fortune = getTodayFortuneResponse.getFortune();
        fortune.setDate(date);

        getTodayFortuneResponse.setFortune(fortune);

        return getTodayFortuneResponse;
    }

    /**
     * 1. 메서드 설명: SecurityContext에서 현재 인증된 사용자 정보를 조회하여 반환하는 메서드.
     * 2. 로직:
     *    - SecurityContextHolder에서 인증 정보를 얻고, 사용자 이름을 조회한다.
     *    - UserRepository를 통해 사용자 엔티티를 검색하며, 없으면 예외 발생.
     * 3. param: 없음
     * 4. return: User (현재 인증된 사용자)
     */
    private User getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        return userRepository.findByGithubName(userName)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    /**
     * 1. 메서드 설명: 입력된 생년월일시와 성별을 기반으로 사주(SajuResult)를 계산하는 메서드.
     * 2. 로직:
     *    - 연주: 생년을 이용해 천간과 지지를 계산한다.
     *    - 월주: 절기 보정을 반영하여 월지를 결정하고, 월간을 계산한다.
     *    - 일주: 기준일(BASE_DATE)부터의 경과일 수를 이용해 일간, 일지를 계산한다.
     *    - 시주: 태어난 시간과 일간을 기반으로 30분 단위 보정을 반영한 시주를 계산한다.
     * 3. param:
     *      birthDateTime - 생년월일과 태어난 시간을 포함하는 LocalDateTime 객체.
     *      sex - 성별 정보.
     * 4. return: SajuResult (계산된 사주의 구성 정보와 성별을 포함)
     */
    private SajuResult calculateSaju(LocalDateTime birthDateTime, String sex) {
        int year = birthDateTime.getYear();
        int yearStemIndex = (year - 4) % 10;
        int yearBranchIndex = (year - 4) % 12;
        String yearPillar = HEAVENLY_STEMS[yearStemIndex] + EARTHLY_BRANCHES[yearBranchIndex];

        String monthBranch = getMonthBranchBySolarTerm(birthDateTime.toLocalDate());
        int monthOrder = getBranchOrder(monthBranch);
        int monthStemIndex = (yearStemIndex * 2 + monthOrder + 1) % 10;
        String monthPillar = HEAVENLY_STEMS[monthStemIndex] + monthBranch;

        LocalDate birthDate = birthDateTime.toLocalDate();
        long daysBetween = ChronoUnit.DAYS.between(BASE_DATE, birthDate);
        int dayIndex = (int) ((daysBetween % 60 + 60) % 60);
        int dayStemIndex = dayIndex % 10;
        int dayBranchIndex = dayIndex % 12;
        String dayPillar = HEAVENLY_STEMS[dayStemIndex] + EARTHLY_BRANCHES[dayBranchIndex];

        String hourPillar = getHourPillar(birthDateTime, dayStemIndex);

        return SajuResult.builder()
                .yearPillar(yearPillar)
                .monthPillar(monthPillar)
                .dayPillar(dayPillar)
                .hourPillar(hourPillar)
                .sex(sex)
                .build();
    }

    /**
     * 1. 메서드 설명: 태어난 시간을 기반으로 30분 단위 보정을 반영하여 시주(시간의 천간과 지지)를 계산하는 메서드.
     * 2. 로직:
     *    - 태어난 시간을 분 단위로 환산한 후, 자시(23:00 기준)로 조정한다.
     *    - 120분(2시간) 단위로 시지를 계산하고, 해당 구간 내 30분 초과 시 오프셋을 적용하여 천간을 산출한다.
     * 3. param:
     *      birthDateTime - 태어난 날짜와 시간.
     *      dayStemIndex - 일간 천간의 인덱스.
     * 4. return: String (계산된 시주의 천간+지지)
     */
    private static String getHourPillar(LocalDateTime birthDateTime, int dayStemIndex) {
        int birthHour = birthDateTime.getHour();
        int birthMinute = birthDateTime.getMinute();
        int totalMinutes = birthHour * 60 + birthMinute;

        int adjustedMinutes = totalMinutes - (23 * 60);
        if (adjustedMinutes < 0) {
            adjustedMinutes += 1440;
        }

        double branchIndexDecimal = adjustedMinutes / 120.0;
        int hourBranchIndex = (int) Math.floor(branchIndexDecimal) % 12;
        String hourBranch = EARTHLY_BRANCHES[hourBranchIndex];

        double segmentFraction = branchIndexDecimal - Math.floor(branchIndexDecimal);
        int halfHourOffset = (segmentFraction >= 0.5) ? 1 : 0;

        int hourStemIndex = ((dayStemIndex % 5) * 2 + hourBranchIndex + halfHourOffset) % 10;
        String hourStem = HEAVENLY_STEMS[hourStemIndex];

        return hourStem + hourBranch;
    }

    /**
     * 1. 메서드 설명: 입력된 출생일을 기준으로 12절기를 고려하여 해당 월지를 결정하는 메서드.
     * 2. 로직:
     *    - 현재 연도의 절기 리스트를 날짜순으로 정렬한 후, 출생일과 비교하여 가장 최근에 시작된 절기를 찾는다.
     *    - 만약 출생일이 해당 연도의 첫 절기보다 이전이면, 전년도 절기 리스트의 마지막 절기를 사용한다.
     * 3. param: birthDate - LocalDate (출생일)
     * 4. return: String (결정된 월지)
     */
    private String getMonthBranchBySolarTerm(LocalDate birthDate) {
        int year = birthDate.getYear();
        List<SolarTerm> currentYearTerms = SOLAR_TERMS.stream()
                .sorted(Comparator.comparing(term -> term.getDate(year)))
                .toList();

        SolarTerm matchedTerm = currentYearTerms.stream()
                .filter(term -> !birthDate.isBefore(term.getDate(year)))
                .reduce((first, second) -> second)
                .orElse(null);

        if (matchedTerm == null) {
            int prevYear = year - 1;
            List<SolarTerm> previousYearTerms = SOLAR_TERMS.stream()
                    .sorted(Comparator.comparing(term -> term.getDate(prevYear)))
                    .toList();
            matchedTerm = previousYearTerms.get(previousYearTerms.size() - 1);
        }
        return matchedTerm.getBranch();
    }

    /**
     * 1. 메서드 설명: 입력된 지지 문자열의 순서(인덱스)를 반환하는 메서드.
     * 2. 로직:
     *    - EARTHLY_BRANCHES 배열을 순회하며 입력 문자열과 일치하는 인덱스를 찾아 반환한다.
     * 3. param: branch - String (지지 문자열)
     * 4. return: int (해당 지지의 인덱스)
     */
    private int getBranchOrder(String branch) {
        for (int i = 0; i < EARTHLY_BRANCHES.length; i++) {
            if (EARTHLY_BRANCHES[i].equals(branch)) {
                return i;
            }
        }
        return 0;
    }
}
