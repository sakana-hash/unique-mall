package io.github.sakana.mockdata.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Mock 数据生成参数，全部可通过启动参数覆盖，例如：--mock.batch-size=2000 --mock.product-count=10000
 */
@Data
@ConfigurationProperties(prefix = "mock")
public class MockDataProperties {

    /** 单条 INSERT 语句包含的行数（batch size），建议 500 ~ 5000 */
    private int batchSize = 1000;

    /** 并行插入线程数 */
    private int threads = 8;

    /** 是否在插入前 TRUNCATE 目标表（破坏性操作，谨慎开启） */
    private boolean truncateFirst = false;

    /** 演练模式：只生成数据不写库，用于验证生成逻辑和吞吐 */
    private boolean dryRun = false;

    /** 商品总数 */
    private long productCount = 1_000_000L;

    /** 每个商品的 SKU 数量下限（默认 2~4，平均 3 个 => 100 万商品约产生 300 万 SKU） */
    private int skuPerProductMin = 2;

    /** 每个商品的 SKU 数量上限 */
    private int skuPerProductMax = 4;

    /** 是否生成 product_detail（与商品 1:1） */
    private boolean detailEnabled = true;

    /** 每个商品的图片数量下限（第 1 张为主图，其余为详情图） */
    private int imagePerProductMin = 2;

    /** 每个商品的图片数量上限 */
    private int imagePerProductMax = 4;

    /** 一级分类数量 */
    private int categoryLevel1Count = 12;

    /** 每个一级分类下的二级分类数量（最多 10 个） */
    private int categoryLevel2PerLevel1 = 8;

    /** created_time 随机分布在最近 N 天内 */
    private long createdWithinDays = 730;

    public void validate() {
        if (batchSize < 1 || batchSize > 20000) {
            throw new IllegalArgumentException("mock.batch-size 必须在 1 ~ 20000 之间，当前: " + batchSize);
        }
        if (threads < 1 || threads > 64) {
            throw new IllegalArgumentException("mock.threads 必须在 1 ~ 64 之间，当前: " + threads);
        }
        if (productCount < 1) {
            throw new IllegalArgumentException("mock.product-count 必须大于 0，当前: " + productCount);
        }
        if (skuPerProductMin < 1 || skuPerProductMax < skuPerProductMin) {
            throw new IllegalArgumentException("mock.sku-per-product-min/max 配置非法: ["
                    + skuPerProductMin + ", " + skuPerProductMax + "]");
        }
        if (imagePerProductMin < 1 || imagePerProductMax < imagePerProductMin) {
            throw new IllegalArgumentException("mock.image-per-product-min/max 配置非法: ["
                    + imagePerProductMin + ", " + imagePerProductMax + "]");
        }
        if (categoryLevel1Count < 1 || categoryLevel1Count > 20) {
            throw new IllegalArgumentException("mock.category-level1-count 必须在 1 ~ 20 之间，当前: " + categoryLevel1Count);
        }
        if (categoryLevel2PerLevel1 < 1 || categoryLevel2PerLevel1 > 10) {
            throw new IllegalArgumentException("mock.category-level2-per-level1 必须在 1 ~ 10 之间，当前: "
                    + categoryLevel2PerLevel1);
        }
        if (createdWithinDays < 1) {
            throw new IllegalArgumentException("mock.created-within-days 必须大于 0，当前: " + createdWithinDays);
        }
    }
}
