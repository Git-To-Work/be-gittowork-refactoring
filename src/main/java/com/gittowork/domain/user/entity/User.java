package com.gittowork.domain.user.entity;

import com.gittowork.domain.coverletter.entity.CoverLetter;
import com.gittowork.domain.coverletter.entity.CoverLetterAnalysis;
import com.gittowork.domain.firebase.entity.UserAlertLog;
import com.gittowork.domain.interaction.entity.UserBlacklist;
import com.gittowork.domain.interaction.entity.UserLikes;
import com.gittowork.domain.interaction.entity.UserScraps;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private Integer id;

    @NotNull
    @Column(name = "github_id", nullable = false)
    private Integer githubId;

    @Size(max = 100)
    @NotNull
    @Column(name = "github_name", nullable = false, length = 100)
    private String githubName;

    @Size(max = 30)
    @Column(name = "name", length = 30)
    private String name;

    @Size(max = 100)
    @Column(name = "github_email", length = 100)
    private String githubEmail;

    @Size(max = 13)
    @Column(name = "phone", length = 13)
    private String phone;

    @Column(name = "birth_dt")
    private LocalDate birthDt;

    @ColumnDefault("0")
    @Column(name = "experience")
    private Integer experience;

    @Size(max = 100)
    @Column(name = "location", length = 100)
    private String location;

    @NotNull
    @Column(name = "create_dttm", nullable = false)
    private LocalDateTime createDttm;

    @Column(name = "update_dttm")
    private LocalDateTime updateDttm;

    @Column(name = "privacy_consent_dttm")
    private LocalDateTime privacyConsentDttm;

    @Size(max = 255)
    @Column(name = "interest_fields")
    private String interestFields;

    @Column(name = "delete_dttm")
    private LocalDateTime deleteDttm;

    @Column(name = "notification_agree_dttm")
    private LocalDateTime notificationAgreeDttm;

    @Size(max = 255)
    @Column(name = "github_access_token")
    private String githubAccessToken;

    @OneToOne(mappedBy = "user")
    private UserGitInfo userGitInfo;

    @Size(max = 255)
    @Column(name = "FCM_token")
    private String fcmToken;

    @OneToMany(mappedBy = "user")
    private Set<CoverLetter> coverLetters = new LinkedHashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<CoverLetterAnalysis> coverLetterAnalyses = new LinkedHashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<UserAlertLog> userAlertLogs = new LinkedHashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<UserBlacklist> userBlacklists = new LinkedHashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<UserLikes> userLikes = new LinkedHashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<UserScraps> userScraps = new LinkedHashSet<>();

    public void setUserGitInfo(UserGitInfo userGitInfo) {
        if (userGitInfo == null) {
            if (this.userGitInfo != null) {
                this.userGitInfo.setUser(null);
            }
        } else {
            userGitInfo.setUser(this);
        }
        this.userGitInfo = userGitInfo;
    }

}
