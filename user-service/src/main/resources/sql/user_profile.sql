create table `user`.`user_profile`
(
    id           bigint                             not null
        primary key,
    user_id      bigint                             not null,
    nickname     varchar(64)                        null,
    avatar       varchar(512)                       null,
    gender       tinyint                            null comment '0未知 1男 2女',
    birthday     date                               null,
    created_time datetime default CURRENT_TIMESTAMP not null,
    updated_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,

    unique key uk_user_id(user_id)
);
