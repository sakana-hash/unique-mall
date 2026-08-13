create table stock.stock_record (

    id bigint primary key comment '库存记录id',

    sku_id bigint not null comment 'sku id',
    order_id bigint comment '关联订单 id',
    change_type tinyint not null comment '流水类型 1初始化库存 2锁定库存 3扣减库存 4释放库存 5人工调整',
    change_amount int not null comment '业务涉及商品数量',

    available_before int not null comment '可用库存调整前数量',
    available_after int not null comment '可用库存调整后数量',

    locked_before int not null comment '锁定库存调整前数量',
    locked_after int not null comment '锁定库存调整后数量',

    created_time datetime not null ,

    index idx_sku_id(sku_id),
    index idx_order_id(order_id)
);