# mock-data-generator

Mock 数据生成与批量插入工具。基于 `product-service` 的建表脚本（`src/main/resources/sql/*.sql`）
和实体类（`Product` / `ProductSKU` / `ProductDetail` / `ProductImage`），使用
`unique-spring-boot-starter` 中的雪花ID生成器（`SnowflakeIdGenerator`）生成主键，
多线程 + 多行 VALUES 批量 INSERT 灌数据。

默认目标数据量：

| 表 | 数量 | 说明 |
|---|---|---|
| product_category | 12 x 8 = 96 | 一级分类 12 个，每个下面 8 个二级分类 |
| product | 100 万 | `mock.product-count` |
| product_sku | ~300 万 | 每个商品 2~4 个 SKU，平均 3 个 |
| product_detail | ~100 万 | 与商品 1:1，可用 `mock.detail-enabled=false` 关闭 |
| product_image | ~300 万 | 每个商品 2~4 张（第 1 张为主图） |

## 快速开始

```bash
# 1. 确保 product 库已执行建表脚本（product-service/src/main/resources/sql/*.sql）
# 2. 打包（含可执行 fat jar）
mvn -pl mock-data-generator -am package -DskipTests

# 3. 先用小规模 + dry-run 验证
java -jar target/mock-data-generator.jar --mock.dry-run=true --mock.product-count=10000

# 4. 正式灌入 100 万商品 / 300 万 SKU
java -jar target/mock-data-generator.jar
```

也可以直接在 IDEA 里运行 `MockDataApplication#main`，参数写在 Run Configuration 的
Program arguments（如 `--mock.batch-size=2000`）。

## 参数一览（均可用 `--xxx` 启动参数覆盖）

| 参数 | 默认值 | 说明 |
|---|---|---|
| `mock.batch-size` | 1000 | 单条 INSERT 语句的行数（batch size），建议 500~5000 |
| `mock.threads` | 8 | 并行插入线程数 |
| `mock.product-count` | 1000000 | 商品总数 |
| `mock.sku-per-product-min/max` | 2 / 4 | 每商品 SKU 数区间 |
| `mock.detail-enabled` | true | 是否生成商品详情 |
| `mock.image-per-product-min/max` | 2 / 4 | 每商品图片数区间 |
| `mock.truncate-first` | false | 插入前 TRUNCATE 五张目标表（破坏性，慎用） |
| `mock.dry-run` | false | 只生成不写库，验证逻辑/吞吐 |
| `mock.category-level1-count` | 12 | 一级分类数量（≤20） |
| `mock.category-level2-per-level1` | 8 | 每个一级分类的二级分类数（≤10） |
| `mock.created-within-days` | 730 | created_time 随机分布在最近 N 天 |

数据库连接在 `src/main/resources/mock-data.yaml`，可用启动参数覆盖：

```bash
java -jar target/mock-data-generator.jar \
  --spring.datasource.url='jdbc:mysql://10.0.0.8:3306/product?...' \
  --spring.datasource.username=root --spring.datasource.password=xxx
```

## 实现要点

- **主键**：全部来自 `SnowflakeIdGenerator`（starter 自动装配）。本工具的
  `worker-id=21` 与 product-service 运行实例（`worker-id=11`）错开，雪花 ID 的
  worker 位不同 => mock 数据与线上数据的 ID 永不重叠。
- **批量插入**：每张表一个 `Mock*Mapper.batchInsert`，XML 里用 `<foreach>` 拼多行
  VALUES，一次调用插 `batch-size` 行；工作线程攒满一批就提交，内存里只保留一批数据。
- **并发**：`mock.threads` 个线程，每个线程处理若干商品分片；`sku_code` 形如
  `SKU-{productId}-{nn}`，由雪花 ID 保证全局唯一。
- **进度**：每 5 秒打印一次各表进度与实时速率，结束后打印总耗时和平均速率。

## 性能建议

- `batch-size=1000~2000`、`threads=8` 时，本地 MySQL 8 通常可达 10~20 万行/秒，
  全量（约 800 万行）约 1~3 分钟。
- 单条多行 INSERT 较大，如把 `batch-size` 调到 5000 以上，请确认
  `max_allowed_packet` ≥ 64M。
- 追求极限速度可对目标库临时关闭 binlog（`SET sql_log_bin=0`，需要 SUPER 权限）
  或调大 `innodb_buffer_pool_size`、`innodb_flush_log_at_trx_commit=2`。
- 失败重试：中途失败不会回滚已写入的批次，可加 `--mock.truncate-first=true` 重跑。
