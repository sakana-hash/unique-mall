create table `product`.`product_sku` (

    id  bigint  primary key ,
    product_id  bigint not null comment '商品ID',
    sku_code    varchar(64) not null comment '业务SKU编码',
    price   bigint  not null comment '价格, 单位分',
    status  tinyint not null default 0  comment '状态 0下架 1上架',
    created_time datetime not null,
    updated_time datetime not null,

    unique key uk_sku_code(sku_code),
    index idx_product(product_id)
);

alter table product.product_sku
    add product_status tinyint not null comment '状态 0下架 1上架';

create index idx_product_status_price
    on product.product_sku (product_status, status, product_id, price);