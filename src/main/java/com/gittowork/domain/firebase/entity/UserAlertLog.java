package com.gittowork.domain.firebase.entity;

import com.gittowork.domain.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "user_alert_log")
public class UserAlertLog {

    @Id
    @Column(name = "alert_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Size(max = 50)
    @NotNull
    @Column(name = "alert_type", nullable = false, length = 50)
    private String alertType;

    @Size(max = 255)
    @NotNull
    @Column(name = "message", nullable  = false)
    private String message;

    @NotNull
    @Column(name = "create_dttm", nullable = false)
    private LocalDateTime createDttm;

    @Size(max = 255)
    @NotNull
    @Column(name = "title", nullable = false)
    private String title;

}
