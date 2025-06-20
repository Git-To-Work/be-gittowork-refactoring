package com.gittowork.domain.fortune.model;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
public class SolarTerm {
    private Integer month;
    private Integer day;
    private String branch;

    public LocalDate getDate(int year) {
        return LocalDate.of(year, month, day);
    }
}
