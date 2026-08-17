package io.github.sakana.product.constant;

/**
 * 商品服务通用业务常量。
 */
public final class ProductConstants {

    public static final int MIN_PAGE_NUMBER = 1;
    public static final int DEFAULT_PAGE_NUMBER = MIN_PAGE_NUMBER;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MIN_PAGE_SIZE = 1;
    public static final int MAX_PAGE_SIZE = 100;

    public static final int MAX_SKU_QUERY_COUNT = 50;
    public static final long MIN_VALID_ID = 1L;

    public static final long PRODUCT_CACHE_TTL_SECONDS = 600L;
    public static final long PAGE_RESULT_CACHE_TTL_SECONDS = 60L;
    public static final long PAGE_RESULT_CACHE_PHYSICAL_TTL_SECONDS = 86_400L;
    public static final int PAGE_CACHE_COLD_START_LOCK_WAIT_SECONDS = 3;

    private ProductConstants() {
    }
}
