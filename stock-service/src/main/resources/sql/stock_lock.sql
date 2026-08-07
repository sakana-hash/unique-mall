create table stock.stock_lock (

    id bigint primary key,

    sku_id bigint not null,
    order_id bigint not null,
    quantity int not null comment '下单数量',
    status tinyint not null default 0 comment '状态 0锁定 1已扣减 2已释放',
    expire_time datetime comment '锁过期时间',

    created_time datetime not null ,
    updated_time datetime not null ,

    unique key uk_order_sku(order_id, sku_id),
    index idx_order_id(order_id)
);