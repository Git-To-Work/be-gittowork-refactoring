package com.gittowork.domain.fortune.model;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
public class SolarTerm {
    private int month;     // 절기가 시작되는 '월'
    private int day;       // 절기가 시작되는 '일'
    private String branch; // 이 절기부터 시작되는 '월지'

    public LocalDate getDate(int year) {
        return LocalDate.of(year, month, day);
    }
}
