create table `user`.`user`
(
    id           bigint                             not null comment '用户ID'
        primary key,
    username     varchar(64)                        not null comment '用户名',
    password     varchar(255)                       not null comment '密码hash',
    phone        varchar(20)                        null comment '手机号',
    email        varchar(128)                       null comment '邮箱',
    status       tinyint  default 1                 not null comment '状态 1正常 0禁用',
    created_time datetime default CURRENT_TIMESTAMP not null,
    updated_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,

    unique key uk_username(username),
    unique key uk_phone(phone),
    unique key uk_email(email)
);