create table `product`.`product_detail` (

    id  bigint primary key ,
    product_id  bigint not null ,
    content text,

    created_time datetime not null,
    updated_time datetime not null,

    unique key uk_product(product_id)
)