CREATE TABLE meetings
(
	id               INTEGER PRIMARY KEY AUTO_INCREMENT,
	guild_id         BIGINT       NOT NULL,
	created_by       BIGINT       NOT NULL,
	participants     ARRAY                 DEFAULT NULL,
	admins           ARRAY                 DEFAULT NULL,
	created_at       TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
	due_at           TIMESTAMP(0) NOT NULL,
	title            VARCHAR(64)  NOT NULL,
	description      VARCHAR(256) NOT NULL,
	language         VARCHAR(64)  NOT NULL,
	category_id	 	 BIGINT 	  NOT NULL,
	log_channel_id   BIGINT       NOT NULL,
	voice_channel_id BIGINT       NOT NULL,
	active           BOOL         NOT NULL DEFAULT TRUE,
	ongoing          BOOl         NOT NULL DEFAULT FALSE
)