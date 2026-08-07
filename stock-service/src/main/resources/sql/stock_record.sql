create table stock.stock_record (

    id bigint primary key comment '库存记录id',

    sku_id bigint not null comment 'sku id',
    order_id bigint comment '关联订单 id',
    change_type tinyint not null comment '流水类型 1初始化库存 2锁定库存 3扣减库存 4释放库存 5人工调整',
    change_amount int not null comment '变化数量',
    before_stock int not null comment '调整前数量',
    after_stock int not null comment '调整后数量',

    created_time datetime not null ,

    index idx_sku_id(sku_id)
);