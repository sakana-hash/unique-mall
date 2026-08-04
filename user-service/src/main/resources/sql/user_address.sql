create table `user`.`user_address`
(
    id           bigint                             not null
        primary key,
    user_id      bigint                             not null,
    receiver     varchar(64)                        null comment '收货人',
    phone        varchar(20)                        null,
    province     varchar(64)                        null,
    city         varchar(64)                        null,
    district     varchar(64)                        null,
    detail       varchar(255)                       null,
    is_default   tinyint  default 0                 not null comment '是否默认地址 0否 1是',
    created_time datetime default CURRENT_TIMESTAMP not null,
    updated_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,

    index idx_user_id(user_id)
);
