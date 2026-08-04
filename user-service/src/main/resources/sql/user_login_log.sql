create table `user`.`user_login_log`
(
    id           bigint                             not null
        primary key,
    user_id      bigint                             not null,
    ip           varchar(64)                        not null,
    device       varchar(128)                       null,
    login_time   datetime                           not null,
    created_time datetime default CURRENT_TIMESTAMP not null,

    index idx_user_time(user_id, login_time)
);