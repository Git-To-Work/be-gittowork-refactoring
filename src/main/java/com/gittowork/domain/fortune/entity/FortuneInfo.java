package com.gittowork.domain.fortune.entity;

import com.gittowork.domain.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "fortune_info")
public class FortuneInfo {

    @Id
    @Column(name = "user_id", nullable = false)
    private Integer id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Column(name = "birth_dt", nullable = false)
    private LocalDate birthDt;

    @NotNull
    @Lob
    @Column(name = "sex", nullable = false)
    private String sex;

    @NotNull
    @Column(name = "time", nullable = false)
    private LocalTime time;

}
