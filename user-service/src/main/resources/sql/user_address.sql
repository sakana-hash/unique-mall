create table `user`.`user_address`
(
    id           bigint            not null
        primary key comment '地址id',

    user_id      bigint            not null comment '用户id',
    receiver     varchar(64)       not null comment '收货人',
    phone        varchar(20)       not null comment '联系电话',
    province     varchar(64)       not null comment '省份',
    city         varchar(64)       not null comment '城市',
    district     varchar(64) comment '行政区',
    detail       varchar(255)      not null comment '详细地址',
    is_default   tinyint default 0 not null comment '是否默认地址 0否 1是',

    created_time datetime          not null,
    updated_time datetime          not null,

    index idx_user_id (user_id)
);