create schema gittowork;
use gittowork;

CREATE TABLE `user_git_info` (
	`user_id`	int	NOT NULL,
	`github_name`	varchar(100)	NOT NULL,
	`profile_url`	varchar(255)	NOT NULL,
	`public_repositories`	int	NOT NULL,
	`followers`	int	NOT NULL,
	`followings`	int	NOT NULL,
	`create_dttm`	TIMESTAMP	NOT NULL,
	`update_dttm`	TIMESTAMP	NULL
);

CREATE TABLE `user_likes` (
	`user_id`	int	NOT NULL,
	`company_id`	int	NOT NULL,
	`delete_dttm`	TIMESTAMP	NULL
);

CREATE TABLE `user_favorites` (
	`user_id`	int	NOT NULL,
	`company_id`	int	NOT NULL,
	`delete_dttm`	TIMESTAMP	NULL
);

CREATE TABLE `user_alert_logs` (
	`alert_id`	int	NOT NULL,
	`user_id`	int	NOT NULL,
	`alert_type`	varchar(50)	NOT NULL,
	`message`	varchar(255)	NOT NULL,
	`create_dttm`	TIMESTAMP	NOT NULL
);

CREATE TABLE `user` (
	`user_id`	int	NOT NULL,
	`github_id`	int	NOT NULL,
	`name`	varchar(30)	NOT NULL,
	`email`	varchar(255)	NOT NULL,
	`phone`	varchar(13)	NULL,
	`birth_dt`	DATE	NULL,
	`experience`	int	NULL	DEFAULT 0,
	`location`	varchar(100)	NULL,
	`create_dttm`	TIMESTAMP	NOT NULL,
	`update_dttm`	TIMESTAMP	NULL,
	`privacy_consent_dttm`	TIMESTAMP	NULL,
	`token`	varchar(255)	NULL,
	`interest_fields`	varchar(255)	NULL,
	`delete_dttm`	TIMESTAMP	NULL
);

CREATE TABLE `fields` (
	`field_id`	int	NOT NULL,
	`field_name`	varchar(100)	NOT NULL,
	`field_img_url`	varchar(255)	NOT NULL
);

CREATE TABLE `user_blacklist` (
	`user_id`	int	NOT NULL,
	`company_id`	int	NOT NULL,
	`delete_dttm`	TIMESTAMP	NULL
);

CREATE TABLE `cover_letter` (
	`file_id`	int	NOT NULL,
	`user_id`	int	NOT NULL,
	`origin_name`	varchar(255)	NOT NULL,
	`file_url`	varchar(255)	NOT NULL,
	`create_dttm`	TIMESTAMP	NOT NULL,
	`title`	varchar(255)	NOT NULL
);

CREATE TABLE `cover_letter_analysis` (
	`cover_letter_analysis_id`	VARCHAR(255)	NOT NULL,
	`file_id`	int	NOT NULL,
	`user_id`	int	NOT NULL,
	`analysis_result`	TEXT	NULL,
	`global_capability`	int	NULL,
	`challenge_spirit`	int	NULL,
	`sincerity`	int	NULL,
	`communication_skill`	int	NULL,
	`achievement_orientation`	int	NULL,
	`responsibility`	int	NULL,
	`honesty`	int	NULL,
	`creativity`	int	NULL,
	`create_dttm`	TIMESTAMP	NULL
);

ALTER TABLE `user_git_info` ADD CONSTRAINT `PK_USER_GIT_INFO` PRIMARY KEY (
	`user_id`
);

ALTER TABLE `user_likes` ADD CONSTRAINT `PK_USER_LIKES` PRIMARY KEY (
	`user_id`
);

ALTER TABLE `user_favorites` ADD CONSTRAINT `PK_USER_FAVORITES` PRIMARY KEY (
	`user_id`
);

ALTER TABLE `user_alert_logs` ADD CONSTRAINT `PK_USER_ALERT_LOGS` PRIMARY KEY (
	`alert_id`
);

ALTER TABLE `user` ADD CONSTRAINT `PK_USER` PRIMARY KEY (
	`user_id`
);

ALTER TABLE `fields` ADD CONSTRAINT `PK_FIELDS` PRIMARY KEY (
	`field_id`
);

ALTER TABLE `user_blacklist` ADD CONSTRAINT `PK_USER_BLACKLIST` PRIMARY KEY (
	`user_id`
);

ALTER TABLE `cover_letter` ADD CONSTRAINT `PK_COVER_LETTER` PRIMARY KEY (
	`file_id`
);

ALTER TABLE `cover_letter_analysis` ADD CONSTRAINT `PK_COVER_LETTER_ANALYSIS` PRIMARY KEY (
	`cover_letter_analysis_id`
);

ALTER TABLE `user_git_info` ADD CONSTRAINT `FK_user_TO_user_git_info_1` FOREIGN KEY (
	`user_id`
)
REFERENCES `user` (
	`user_id`
);

ALTER TABLE `user_likes` ADD CONSTRAINT `FK_user_TO_user_likes_1` FOREIGN KEY (
	`user_id`
)
REFERENCES `user` (
	`user_id`
);

ALTER TABLE `user_favorites` ADD CONSTRAINT `FK_user_TO_user_favorites_1` FOREIGN KEY (
	`user_id`
)
REFERENCES `user` (
	`user_id`
);

ALTER TABLE `user_alert_logs` ADD CONSTRAINT `FK_user_TO_user_alert_logs_1` FOREIGN KEY (
	`user_id`
)
REFERENCES `user` (
	`user_id`
);

ALTER TABLE `user_blacklist` ADD CONSTRAINT `FK_user_TO_user_blacklist_1` FOREIGN KEY (
	`user_id`
)
REFERENCES `user` (
	`user_id`
);

ALTER TABLE `cover_letter` ADD CONSTRAINT `FK_user_TO_cover_letter_1` FOREIGN KEY (
	`user_id`
)
REFERENCES `user` (
	`user_id`
);

ALTER TABLE `cover_letter_analysis` ADD CONSTRAINT `FK_cover_letter_TO_cover_letter_analysis_1` FOREIGN KEY (
	`file_id`
)
REFERENCES `cover_letter` (
	`file_id`
);

ALTER TABLE `cover_letter_analysis` ADD CONSTRAINT `FK_user_TO_cover_letter_analysis_1` FOREIGN KEY (
	`user_id`
)
REFERENCES `user` (
	`user_id`
);

