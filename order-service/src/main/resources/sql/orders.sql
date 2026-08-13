create table orders.orders
(

    id             bigint primary key comment '订单id',
    order_no       varchar(64) not null comment '订单号',

    user_id        bigint      not null comment '用户id',
    total_amount   bigint      not null comment '订单原始金额，单位分',
    pay_amount     bigint      not null comment '实际应付金额，单位分',
    status         tinyint     not null comment '订单状态: 0待支付 1派送中 2已取消 3已完成',
    remark         varchar(255) comment '用户备注',

    expire_time    datetime comment '支付过期时间',
    paid_time      datetime comment '支付时间',
    cancelled_time datetime comment '取消时间',
    completed_time datetime comment '完成时间',

    created_time   datetime    not null,
    updated_time   datetime    not null,

    unique key uk_order_no (order_no)
)