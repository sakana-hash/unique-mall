create table `product`.`product_category` (

    id  bigint  primary key ,
    parent_id   bigint  default 0 comment '父分类',
    name    varchar(64) not null comment '分类名称',
    level   tinyint not null comment '层级',
    status  tinyint not null default 0 comment '状态 0不可见 1可见',

    created_time datetime not null,
    updated_time datetime not null
)