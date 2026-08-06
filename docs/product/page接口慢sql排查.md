```mysql
select
    p.id

from
    product.product p
        
left join
    product.product_sku ps
on
    p.id = ps.product_id

where
    p.status = 1

group by
    p.id

order by
    min(ps.price) desc 
limit
    0, 20;
```

当前commit：218042c50c2d2c372634120e2220a4d555de62ee

问题：该sql先join再聚合的模式下，会生成接近300万的临时表，严重拖垮性能，查询耗时高达11s，接口不可用

分析：本质原因在于product表没有价格，无法根据价格进行排序，必须join SKU表

方案1: product表增加min_price冗余字段，不再需要join sku  
优点：全部走索引没有任何临时表,性能最佳，查询最快  
缺点：product需要维护min_price的一致性，增加了代码复杂度，如果价格频繁变动会导致锁竞争

方案2: sku表增加product_status字段，先在sku表中查询价格最低的20个商品再join  
优点：避免了价格频繁变动导致的锁竞争，product职责更加单一(只维护商品的最基本信息，避免频繁变动)  
缺点：sku需要维护product_status字段的一致性, sku表中需要先聚合产生临时表再排序影响性能(500ms)

考虑价格可能频繁变动，而商品上下架的频率不大，500ms延迟勉强可以接受，方案2胜出

