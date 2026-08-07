create table stock.stock (

    id bigint primary key comment '库存id',
    sku_id bigint not null comment 'SKU id',
    total_stock int not null default 0 comment '总库存',
    available_stock int not null default 0 comment '可用库存',
    locked_stock int not null default 0 comment '锁定库存',
    version int not null default 0 comment '乐观锁版本号',

    created_time datetime not null ,
    updated_time datetime not null ,

    unique key uk_sku_id(sku_id)
)