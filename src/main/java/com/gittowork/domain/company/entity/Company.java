package com.gittowork.domain.company.entity;

import com.gittowork.domain.field.entity.Field;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "company")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @Column(name = "company_id", nullable = false)
    private Integer id;

    @Size(max = 100)
    @NotNull
    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Size(max = 255)
    @Column(name = "logo")
    private String logo;

    @Column(name = "head_count")
    private Integer headCount;

    @Column(name = "all_avg_salary")
    private Integer allAvgSalary;

    @Column(name = "newcomer_avg_salary")
    private Integer newcomerAvgSalary;

    @ColumnDefault("0")
    @Column(name = "likes")
    private Integer likes;

    @Column(name = "total_sales_value")
    private Integer totalSalesValue;

    @Column(name = "employee_ratio_male")
    private Integer employeeRatioMale;

    @Column(name = "employee_ratio_female")
    private Integer employeeRatioFemale;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "field_id", nullable = false)
    private Field field;

}
