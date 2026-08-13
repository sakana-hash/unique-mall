create table orders.order_status_log
(

    id            bigint primary key,
    order_id      bigint   not null comment '订单id',
    from_status   tinyint comment '原状态',
    to_status     tinyint comment '新状态',
    operator_type tinyint comment '操作来源 1用户 2系统 3支付回调 4管理员',
    remark        varchar(255) comment '备注',

    created_time  datetime not null,

    index idx_order_id (order_id)
)