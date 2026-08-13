create table orders.order_item
(

    id           bigint primary key comment '订单明细id',
    order_id     bigint       not null comment '订单id',
    sku_id       bigint       not null comment 'sku id',
    product_id   bigint       not null comment '商品id',
    product_name varchar(128) not null comment '商品名称快照',
    sku_code     varchar(64) comment 'sku编码快照',
    image_url    varchar(512) comment '商品主图快照',
    price        bigint       not null comment '下单单价，单位分',
    quantity     int          not null comment '购买数量',
    amount       bigint       not null comment '明细金额，单位分',

    created_time datetime     not null,

    index idx_order_id (order_id)
)