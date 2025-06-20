package com.gittowork.domain.fortune.service;

import com.gittowork.domain.fortune.model.SajuResult;
import com.gittowork.domain.fortune.model.SolarTerm;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 사주(四柱) 명리 계산을 담당하는 컴포넌트입니다.
 * <p>
 * 사용자의 생년월일과 시를 바탕으로 년주, 월주, 일주, 시주를 계산하여
 * {@link SajuResult} 객체로 반환합니다.
 * </p>
 */
@Component
public class SajuCalculator {

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
     * 사용자의 생년월일과 성별을 기반으로 사주 명리(년·월·일·시)를 계산합니다.
     *
     * @param birthDateTime 사용자 생년월일 및 시간 정보
     * @param sex 성별 정보 (예: "M", "F")
     * @return 계산된 사주 정보를 담은 {@link SajuResult}
     */
    public SajuResult calculateSaju(LocalDateTime birthDateTime, String sex) {
        // 년주 계산
        int year = birthDateTime.getYear();
        int yearStemIndex = (year - 4) % 10;
        int yearBranchIndex = (year - 4) % 12;
        String yearPillar = HEAVENLY_STEMS[yearStemIndex] + EARTHLY_BRANCHES[yearBranchIndex];

        // 월주 계산 (절기 기준)
        String monthBranch = getMonthBranchBySolarTerm(birthDateTime.toLocalDate());
        int monthOrder = getBranchOrder(monthBranch);
        int monthStemIndex = (yearStemIndex * 2 + monthOrder + 1) % 10;
        String monthPillar = HEAVENLY_STEMS[monthStemIndex] + monthBranch;

        // 일주 계산
        LocalDate birthDate = birthDateTime.toLocalDate();
        long daysBetween = ChronoUnit.DAYS.between(BASE_DATE, birthDate);
        int dayIndex = (int) ((daysBetween % 60 + 60) % 60);
        int dayStemIndex = dayIndex % 10;
        int dayBranchIndex = dayIndex % 12;
        String dayPillar = HEAVENLY_STEMS[dayStemIndex] + EARTHLY_BRANCHES[dayBranchIndex];

        // 시주 계산
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
     * 시간(시주)을 계산합니다.
     *
     * @param birthDateTime 사용자 생년월일 및 시간 정보
     * @param dayStemIndex 일간(날짜) 천간 인덱스
     * @return 계산된 시주 문자열
     */
    private static String getHourPillar(LocalDateTime birthDateTime, int dayStemIndex) {
        int totalMinutes = birthDateTime.getHour() * 60 + birthDateTime.getMinute();
        int adjusted = totalMinutes - (23 * 60);
        if (adjusted < 0) adjusted += 1440;

        double branchIndexDecimal = adjusted / 120.0;
        int branchIndex = (int) Math.floor(branchIndexDecimal) % 12;
        String branch = EARTHLY_BRANCHES[branchIndex];

        int halfHourOffset = (branchIndexDecimal - Math.floor(branchIndexDecimal) >= 0.5) ? 1 : 0;
        int stemIndex = ((dayStemIndex % 5) * 2 + branchIndex + halfHourOffset) % 10;
        String stem = HEAVENLY_STEMS[stemIndex];

        return stem + branch;
    }

    /**
     * 절기(24절기) 정보를 기반으로 월간(월주) 지지를 결정합니다.
     *
     * @param birthDate 사용자 생년월일
     * @return 월간 지지 문자열
     */
    private String getMonthBranchBySolarTerm(LocalDate birthDate) {
        int year = birthDate.getYear();
        List<SolarTerm> terms = SOLAR_TERMS.stream()
                .sorted(Comparator.comparing(term -> term.getDate(year)))
                .toList();
        SolarTerm term = terms.stream()
                .filter(t -> !birthDate.isBefore(t.getDate(year)))
                .reduce((first, second) -> second)
                .orElseGet(() -> SOLAR_TERMS.get(SOLAR_TERMS.size() - 1));
        return term.getBranch();
    }

    /**
     * 지지 배열에서 특정 지지의 순서를 반환합니다.
     *
     * @param branch 지지 문자열
     * @return 지지 순서 인덱스
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
