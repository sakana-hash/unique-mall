create table `product`.`product_image` (

    id  bigint primary key ,
    product_id bigint not null ,
    url varchar(512) not null ,
    type tinyint not null comment '1主图 2详情图',
    sort int default 0,

    created_time datetime not null,

    index idx_product(product_id)
)