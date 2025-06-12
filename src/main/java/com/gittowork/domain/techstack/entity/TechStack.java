package com.gittowork.domain.techstack.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tech_stack")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechStack {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tech_stack_id")
    private Integer id;

    @Column(name = "tech_stack_name", nullable = false, length = 100)
    private String techStackName;
}
