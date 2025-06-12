package com.gittowork.domain.benefit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name="benefit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Benefit {
    @Id
    @Column(name = "benefit_id", nullable = false)
    private Integer id;

    @Size(max = 255)
    @NotNull
    @Column(name = "benefit_name", nullable = false)
    private String benefitName;

}
