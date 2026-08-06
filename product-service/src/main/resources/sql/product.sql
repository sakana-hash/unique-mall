create table `product`.`product`
(
    id           bigint                             not null comment '商品ID'
        primary key,
    category_id  bigint                             not null comment '分类ID',
    name         varchar(128)                       not null comment '商品名称',
    subtitle     varchar(255)                       comment '副标题',
    brand        varchar(64)                        comment '品牌',
    status       tinyint  default 0                 not null comment '状态 0下架 1上架',
    created_time datetime not null,
    updated_time datetime not null,

    index idx_category(category_id)
);

create index idx_status_created
    on product.product (status, created_time);

create index idx_status_category
    on product.product (status, category_id);