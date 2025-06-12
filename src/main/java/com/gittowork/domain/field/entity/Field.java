package com.gittowork.domain.field.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "field")
public class Field {
    @Id
    @Column(name = "field_id", nullable = false)
    private Integer id;

    @Size(max = 100)
    @NotNull
    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @Size(max = 255)
    @NotNull
    @Column(name = "field_logo_url", nullable = false)
    private String fieldLogoUrl;

}
