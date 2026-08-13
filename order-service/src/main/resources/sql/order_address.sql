create table orders.order_address
(

    id           bigint primary key comment '订单地址id',
    order_id     bigint       not null comment '订单id',
    receiver     varchar(64)  not null comment '收货人',
    phone        varchar(20)  not null comment '联系电话',
    province     varchar(64)  not null comment '省份',
    city         varchar(64)  not null comment '城市',
    district     varchar(64) comment '行政区',
    detail       varchar(255) not null comment '详细地址',

    created_time datetime     not null,

    unique key uk_order_id (order_id)
)