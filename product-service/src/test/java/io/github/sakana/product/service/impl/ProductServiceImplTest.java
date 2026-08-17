package io.github.sakana.product.service.impl;

import io.github.sakana.common.exception.BusinessException;
import io.github.sakana.product.constant.ImageType;
import io.github.sakana.product.constant.OnSaleType;
import io.github.sakana.product.mapper.ProductDetailMapper;
import io.github.sakana.product.mapper.ProductImageMapper;
import io.github.sakana.product.mapper.ProductMapper;
import io.github.sakana.product.mapper.ProductSKUMapper;
import io.github.sakana.product.enumeration.PageSort;
import io.github.sakana.product.pojo.dto.ProductPageDTO;
import io.github.sakana.product.pojo.entity.PageResult;
import io.github.sakana.product.pojo.entity.Product;
import io.github.sakana.product.pojo.entity.ProductDetail;
import io.github.sakana.product.pojo.entity.ProductImage;
import io.github.sakana.product.pojo.entity.ProductSKU;
import io.github.sakana.product.pojo.vo.PageVO;
import io.github.sakana.product.pojo.vo.ProductVO;
import io.github.sakana.product.service.CacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static io.github.sakana.product.constant.ProductConstants.DEFAULT_PAGE_NUMBER;
import static io.github.sakana.product.constant.ProductConstants.MAX_PAGE_SIZE;
import static io.github.sakana.product.constant.ProductConstants.MAX_SKU_QUERY_COUNT;
import static io.github.sakana.product.constant.ProductConstants.MIN_PAGE_NUMBER;
import static io.github.sakana.product.constant.ProductConstants.MIN_VALID_ID;
import static io.github.sakana.product.constant.ProductConstants.PAGE_CACHE_COLD_START_LOCK_WAIT_SECONDS;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductSKUMapper skuMapper;
    @Mock
    private ProductDetailMapper detailMapper;
    @Mock
    private ProductImageMapper imageMapper;
    @Mock
    private CacheService cacheService;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock pageCacheLock;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    @DisplayName("分页请求为空时返回400业务异常")
    void shouldRejectNullPageRequest() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.page(null)
        );

        assertBusinessError(
                exception, "PRODUCT_PAGE_REQUEST_REQUIRED", "分页查询参数不能为空", 400
        );
        verifyNoInteractions(cacheService, productMapper, detailMapper, imageMapper, skuMapper);
    }

    @ParameterizedTest
    @ValueSource(longs = {MIN_VALID_ID - 1, MIN_VALID_ID - 2})
    @DisplayName("商品分类ID不合法时返回400业务异常")
    void shouldRejectInvalidCategoryId(Long categoryId) {
        ProductPageDTO request = new ProductPageDTO();
        request.setCategoryId(categoryId);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.page(request)
        );

        assertBusinessError(
                exception, "PRODUCT_CATEGORY_ID_INVALID", "商品分类ID不合法", 400
        );
        verifyNoInteractions(cacheService, productMapper, detailMapper, imageMapper, skuMapper);
    }

    @Test
    @DisplayName("分页参数越界时沿用原规则归一化")
    void shouldNormalizePageParameters() {
        ProductPageDTO request = new ProductPageDTO();
        request.setPage(MIN_PAGE_NUMBER - 1);
        request.setSize(MAX_PAGE_SIZE + 1);
        request.setSort(PageSort.DEFAULT);
        PageResult cachedResult = PageResult.builder()
                .ids(List.of())
                .total(0L)
                .expireTime(LocalDateTime.now().plusMinutes(1))
                .build();
        when(cacheService.buildPageResultKey(
                DEFAULT_PAGE_NUMBER, MAX_PAGE_SIZE, null, PageSort.DEFAULT
        ))
                .thenReturn("page-key");
        when(cacheService.getPageResult("page-key")).thenReturn(cachedResult);

        PageVO<ProductVO> result = productService.page(request);

        assertEquals(DEFAULT_PAGE_NUMBER, result.getPage());
        assertEquals(MAX_PAGE_SIZE, result.getSize());
        assertEquals(0L, result.getTotal());
        assertEquals(0L, result.getPages());
        assertEquals(List.of(), result.getItems());
        verifyNoInteractions(redissonClient);
    }

    @Test
    @DisplayName("分页缓存过期且未获取锁时立即返回旧数据")
    void shouldReturnStalePageResultWhenRefreshLockIsHeld() {
        ProductPageDTO request = new ProductPageDTO();
        PageResult staleResult = PageResult.builder()
                .ids(List.of())
                .total(0L)
                .expireTime(LocalDateTime.now().minusMinutes(1))
                .build();
        when(cacheService.buildPageResultKey(
                DEFAULT_PAGE_NUMBER, request.getSize(), null, PageSort.DEFAULT
        )).thenReturn("page-key");
        when(cacheService.getPageResult("page-key")).thenReturn(staleResult);
        when(redissonClient.getLock("lock:page-key")).thenReturn(pageCacheLock);
        when(pageCacheLock.tryLock()).thenReturn(false);

        PageVO<ProductVO> result = productService.page(request);

        assertEquals(0L, result.getTotal());
        assertEquals(List.of(), result.getItems());
        verify(pageCacheLock).tryLock();
        verifyNoInteractions(productMapper, detailMapper, imageMapper, skuMapper);
    }

    @Test
    @DisplayName("分页缓存过期且获取锁时同步重建缓存")
    void shouldRebuildExpiredPageResultSynchronously() {
        ProductPageDTO request = new ProductPageDTO();
        PageResult staleResult = PageResult.builder()
                .ids(List.of())
                .total(0L)
                .expireTime(LocalDateTime.now().minusMinutes(1))
                .build();
        when(cacheService.buildPageResultKey(
                DEFAULT_PAGE_NUMBER, request.getSize(), null, PageSort.DEFAULT
        )).thenReturn("page-key");
        when(cacheService.getPageResult("page-key"))
                .thenReturn(staleResult, staleResult);
        when(redissonClient.getLock("lock:page-key")).thenReturn(pageCacheLock);
        when(pageCacheLock.tryLock()).thenReturn(true);
        when(pageCacheLock.isHeldByCurrentThread()).thenReturn(true);
        when(productMapper.selectPage(any())).thenReturn(List.of());
        when(productMapper.count(any())).thenReturn(0L);

        PageVO<ProductVO> result = productService.page(request);

        assertEquals(0L, result.getTotal());
        verify(cacheService).setPageResult(any(), any(PageResult.class));
        verify(pageCacheLock).unlock();
    }

    @Test
    @DisplayName("分页缓存过期且获取锁后发现新缓存时不重复执行SQL")
    void shouldReuseFreshPageResultAfterRefreshLockAcquired() {
        ProductPageDTO request = new ProductPageDTO();
        PageResult staleResult = PageResult.builder()
                .ids(List.of())
                .total(0L)
                .expireTime(LocalDateTime.now().minusMinutes(1))
                .build();
        PageResult freshResult = PageResult.builder()
                .ids(List.of())
                .total(0L)
                .expireTime(LocalDateTime.now().plusMinutes(1))
                .build();
        when(cacheService.buildPageResultKey(
                DEFAULT_PAGE_NUMBER, request.getSize(), null, PageSort.DEFAULT
        )).thenReturn("page-key");
        when(cacheService.getPageResult("page-key"))
                .thenReturn(staleResult, freshResult);
        when(redissonClient.getLock("lock:page-key")).thenReturn(pageCacheLock);
        when(pageCacheLock.tryLock()).thenReturn(true);
        when(pageCacheLock.isHeldByCurrentThread()).thenReturn(true);

        PageVO<ProductVO> result = productService.page(request);

        assertEquals(0L, result.getTotal());
        verify(pageCacheLock).unlock();
        verifyNoInteractions(productMapper, detailMapper, imageMapper, skuMapper);
    }

    @Test
    @DisplayName("冷缓存等待获取锁后发现缓存已重建时不重复执行SQL")
    void shouldReusePageResultAfterColdStartLockAcquired() throws InterruptedException {
        ProductPageDTO request = new ProductPageDTO();
        PageResult freshResult = PageResult.builder()
                .ids(List.of())
                .total(0L)
                .expireTime(LocalDateTime.now().plusMinutes(1))
                .build();
        when(cacheService.buildPageResultKey(
                DEFAULT_PAGE_NUMBER, request.getSize(), null, PageSort.DEFAULT
        )).thenReturn("page-key");
        when(cacheService.getPageResult("page-key"))
                .thenReturn(null, freshResult);
        when(redissonClient.getLock("lock:page-key")).thenReturn(pageCacheLock);
        when(pageCacheLock.tryLock(
                PAGE_CACHE_COLD_START_LOCK_WAIT_SECONDS, TimeUnit.SECONDS
        )).thenReturn(true);
        when(pageCacheLock.isHeldByCurrentThread()).thenReturn(true);

        PageVO<ProductVO> result = productService.page(request);

        assertEquals(0L, result.getTotal());
        verify(pageCacheLock).unlock();
        verifyNoInteractions(productMapper, detailMapper, imageMapper, skuMapper);
    }

    @Test
    @DisplayName("冷缓存第一个请求获取锁后重建缓存")
    void shouldRebuildColdPageCache() throws InterruptedException {
        ProductPageDTO request = new ProductPageDTO();
        when(cacheService.buildPageResultKey(
                DEFAULT_PAGE_NUMBER, request.getSize(), null, PageSort.DEFAULT
        )).thenReturn("page-key");
        when(cacheService.getPageResult("page-key")).thenReturn(null);
        when(redissonClient.getLock("lock:page-key")).thenReturn(pageCacheLock);
        when(pageCacheLock.tryLock(
                PAGE_CACHE_COLD_START_LOCK_WAIT_SECONDS, TimeUnit.SECONDS
        )).thenReturn(true);
        when(pageCacheLock.isHeldByCurrentThread()).thenReturn(true);
        when(productMapper.selectPage(any())).thenReturn(List.of());
        when(productMapper.count(any())).thenReturn(0L);

        PageVO<ProductVO> result = productService.page(request);

        assertEquals(0L, result.getTotal());
        verify(cacheService).setPageResult(any(), any(PageResult.class));
        verify(pageCacheLock).unlock();
    }

    @Test
    @DisplayName("冷缓存等待锁超时且仍无缓存时结束等待")
    void shouldFailWhenColdCacheLockTimesOut() throws InterruptedException {
        ProductPageDTO request = new ProductPageDTO();
        when(cacheService.buildPageResultKey(
                DEFAULT_PAGE_NUMBER, request.getSize(), null, PageSort.DEFAULT
        )).thenReturn("page-key");
        when(cacheService.getPageResult("page-key")).thenReturn(null);
        when(redissonClient.getLock("lock:page-key")).thenReturn(pageCacheLock);
        when(pageCacheLock.tryLock(
                PAGE_CACHE_COLD_START_LOCK_WAIT_SECONDS, TimeUnit.SECONDS
        )).thenReturn(false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> productService.page(request)
        );

        assertEquals("等待分页缓存初始化超时", exception.getMessage());
        verify(pageCacheLock, never()).unlock();
        verifyNoInteractions(productMapper, detailMapper, imageMapper, skuMapper);
    }

    @Test
    @DisplayName("冷缓存等待锁超时但缓存已重建时直接返回")
    void shouldReturnRebuiltPageResultWhenColdCacheLockTimesOut()
            throws InterruptedException {
        ProductPageDTO request = new ProductPageDTO();
        PageResult freshResult = PageResult.builder()
                .ids(List.of())
                .total(0L)
                .expireTime(LocalDateTime.now().plusMinutes(1))
                .build();
        when(cacheService.buildPageResultKey(
                DEFAULT_PAGE_NUMBER, request.getSize(), null, PageSort.DEFAULT
        )).thenReturn("page-key");
        when(cacheService.getPageResult("page-key"))
                .thenReturn(null, freshResult);
        when(redissonClient.getLock("lock:page-key")).thenReturn(pageCacheLock);
        when(pageCacheLock.tryLock(
                PAGE_CACHE_COLD_START_LOCK_WAIT_SECONDS, TimeUnit.SECONDS
        )).thenReturn(false);

        PageVO<ProductVO> result = productService.page(request);

        assertEquals(0L, result.getTotal());
        verify(pageCacheLock, never()).unlock();
        verifyNoInteractions(productMapper, detailMapper, imageMapper, skuMapper);
    }

    @Test
    @DisplayName("冷缓存等待锁被中断时恢复线程中断状态")
    void shouldRestoreInterruptStatusWhenColdCacheWaitIsInterrupted()
            throws InterruptedException {
        ProductPageDTO request = new ProductPageDTO();
        when(cacheService.buildPageResultKey(
                DEFAULT_PAGE_NUMBER, request.getSize(), null, PageSort.DEFAULT
        )).thenReturn("page-key");
        when(cacheService.getPageResult("page-key")).thenReturn(null);
        when(redissonClient.getLock("lock:page-key")).thenReturn(pageCacheLock);
        when(pageCacheLock.tryLock(
                PAGE_CACHE_COLD_START_LOCK_WAIT_SECONDS, TimeUnit.SECONDS
        )).thenThrow(new InterruptedException("interrupted"));

        try {
            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> productService.page(request)
            );

            assertEquals("等待分页缓存初始化时线程被中断", exception.getMessage());
            assertTrue(Thread.currentThread().isInterrupted());
            verify(pageCacheLock, never()).unlock();
            verifyNoInteractions(productMapper, detailMapper, imageMapper, skuMapper);
        } finally {
            Thread.interrupted();
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("SKU ID列表为空时返回400业务异常")
    void shouldRejectEmptySkuIds(List<Long> skuIds) {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.getSkuTradeInfo(skuIds)
        );

        assertBusinessError(
                exception, "PRODUCT_SKU_IDS_REQUIRED", "SKU ID列表不能为空", 400
        );
        verifyNoInteractions(skuMapper, productMapper, detailMapper, imageMapper, cacheService);
    }

    @Test
    @DisplayName("SKU查询数量超过上限时返回400并携带数量限制")
    void shouldRejectTooManySkuIds() {
        int currentCount = MAX_SKU_QUERY_COUNT + 1;
        List<Long> skuIds = LongStream.range(
                MIN_VALID_ID, MIN_VALID_ID + currentCount
        ).boxed().toList();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.getSkuTradeInfo(skuIds)
        );

        assertBusinessError(
                exception,
                "PRODUCT_SKU_QUERY_LIMIT_EXCEEDED",
                "单次最多查询" + MAX_SKU_QUERY_COUNT + "个SKU",
                400
        );
        assertEquals(Map.of(
                "currentCount", currentCount,
                "maxCount", MAX_SKU_QUERY_COUNT
        ), exception.getDetails());
        verifyNoInteractions(skuMapper, productMapper, detailMapper, imageMapper, cacheService);
    }

    @ParameterizedTest
    @MethodSource("invalidSkuIdLists")
    @DisplayName("SKU ID不合法时返回400并携带元素位置")
    void shouldRejectInvalidSkuId(List<Long> skuIds) {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.getSkuTradeInfo(skuIds)
        );

        assertBusinessError(exception, "PRODUCT_SKU_ID_INVALID", "SKU ID不合法", 400);
        assertEquals(Map.of("index", 1), exception.getDetails());
        verifyNoInteractions(skuMapper, productMapper, detailMapper, imageMapper, cacheService);
    }

    @Test
    @DisplayName("SKU ID重复时返回400并携带重复ID")
    void shouldRejectDuplicatedSkuId() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.getSkuTradeInfo(List.of(1001L, 1001L))
        );

        assertBusinessError(
                exception, "PRODUCT_SKU_ID_DUPLICATED", "SKU ID不能重复", 400
        );
        assertEquals(Map.of("skuId", 1001L), exception.getDetails());
        verifyNoInteractions(skuMapper, productMapper, detailMapper, imageMapper, cacheService);
    }

    @Test
    @DisplayName("部分SKU不存在时返回404并携带缺失ID")
    void shouldRejectMissingSku() {
        ProductSKU existingSku = sku(
                1001L, 2001L, OnSaleType.ONSALE, OnSaleType.ONSALE
        );
        when(skuMapper.selectByIds(List.of(1001L, 1002L))).thenReturn(List.of(existingSku));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.getSkuTradeInfo(List.of(1001L, 1002L))
        );

        assertBusinessError(exception, "PRODUCT_SKU_NOT_FOUND", "部分SKU不存在", 404);
        assertEquals(Map.of("skuIds", List.of(1002L)), exception.getDetails());
        verifyNoInteractions(productMapper, detailMapper, imageMapper, cacheService);
    }

    @Test
    @DisplayName("部分SKU不可销售时返回409并携带对应ID")
    void shouldRejectUnavailableSku() {
        ProductSKU unavailableSku = sku(
                1001L, 2001L, OnSaleType.OFFSALE, OnSaleType.ONSALE
        );
        when(skuMapper.selectByIds(List.of(1001L))).thenReturn(List.of(unavailableSku));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.getSkuTradeInfo(List.of(1001L))
        );

        assertBusinessError(
                exception, "PRODUCT_SKU_NOT_AVAILABLE", "部分SKU不可销售", 409
        );
        assertEquals(Map.of("skuIds", List.of(1001L)), exception.getDetails());
        verifyNoInteractions(productMapper, detailMapper, imageMapper, cacheService);
    }

    @Test
    @DisplayName("SKU交易信息查询成功时补充商品名称和主图")
    void shouldReturnSkuTradeInfo() {
        ProductSKU sku = sku(1001L, 2001L, OnSaleType.ONSALE, OnSaleType.ONSALE);
        Product product = productWithMainImage(2001L);
        product.setName("测试商品");
        when(skuMapper.selectByIds(List.of(1001L))).thenReturn(List.of(sku));
        when(cacheService.getProducts(List.of(2001L))).thenReturn(List.of(product));

        List<ProductSKU> result = productService.getSkuTradeInfo(List.of(1001L));

        assertEquals(1, result.size());
        assertSame(sku, result.get(0));
        assertEquals("测试商品", sku.getProductName());
        assertEquals("https://example.com/main.jpg", sku.getImageUrl());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {MIN_VALID_ID - 1, MIN_VALID_ID - 2})
    @DisplayName("商品ID不合法时返回400业务异常")
    void shouldRejectInvalidProductId(Long id) {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.getDetail(id)
        );

        assertBusinessError(exception, "PRODUCT_ID_INVALID", "商品ID不合法", 400);
        verifyNoInteractions(cacheService, productMapper, detailMapper, imageMapper, skuMapper);
    }

    @Test
    @DisplayName("商品不存在时返回404并携带商品ID")
    void shouldRejectMissingProduct() {
        when(cacheService.getProduct(1001L)).thenReturn(null);
        when(productMapper.selectById(1001L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.getDetail(1001L)
        );

        assertBusinessError(exception, "PRODUCT_NOT_FOUND", "商品不存在", 404);
        assertEquals(Map.of("productId", 1001L), exception.getDetails());
        verifyNoInteractions(detailMapper, imageMapper, skuMapper);
    }

    @Test
    @DisplayName("数据库中的商品已下架时返回409")
    void shouldRejectProductNotOnSaleFromDatabase() {
        Product product = product(1001L, OnSaleType.OFFSALE);
        when(cacheService.getProduct(1001L)).thenReturn(null);
        when(productMapper.selectById(1001L)).thenReturn(product);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.getDetail(1001L)
        );

        assertBusinessError(exception, "PRODUCT_NOT_ON_SALE", "商品已下架", 409);
        assertEquals(Map.of("productId", 1001L), exception.getDetails());
        verifyNoInteractions(detailMapper, imageMapper, skuMapper);
    }

    @Test
    @DisplayName("缓存中的商品已下架时也返回409")
    void shouldRejectProductNotOnSaleFromCache() {
        when(cacheService.getProduct(1001L))
                .thenReturn(product(1001L, OnSaleType.OFFSALE));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.getDetail(1001L)
        );

        assertBusinessError(exception, "PRODUCT_NOT_ON_SALE", "商品已下架", 409);
        verifyNoInteractions(productMapper, detailMapper, imageMapper, skuMapper);
    }

    @Test
    @DisplayName("缓存命中在售商品时直接返回")
    void shouldReturnCachedProduct() {
        Product cachedProduct = product(1001L, OnSaleType.ONSALE);
        when(cacheService.getProduct(1001L)).thenReturn(cachedProduct);

        Product result = productService.getDetail(1001L);

        assertSame(cachedProduct, result);
        verifyNoInteractions(productMapper, detailMapper, imageMapper, skuMapper);
    }

    @Test
    @DisplayName("缓存未命中时组装商品详情并回写缓存")
    void shouldAssembleAndCacheProduct() {
        Product product = product(1001L, OnSaleType.ONSALE);
        ProductDetail detail = new ProductDetail();
        detail.setContent("商品详情");
        List<ProductImage> images = List.of(new ProductImage());
        List<ProductSKU> skus = List.of(new ProductSKU());
        when(cacheService.getProduct(1001L)).thenReturn(null);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(detailMapper.selectByProductId(1001L)).thenReturn(detail);
        when(imageMapper.selectByProductId(1001L)).thenReturn(images);
        when(skuMapper.selectByProductId(1001L)).thenReturn(skus);

        Product result = productService.getDetail(1001L);

        assertSame(product, result);
        assertEquals("商品详情", result.getContent());
        assertSame(images, result.getImages());
        assertSame(skus, result.getSkus());
        verify(cacheService).setProduct(product);
    }

    private static Product product(Long id, Integer status) {
        Product product = new Product();
        product.setId(id);
        product.setStatus(status);
        return product;
    }

    private static Product productWithMainImage(Long id) {
        Product product = product(id, OnSaleType.ONSALE);
        ProductImage image = new ProductImage();
        image.setProductId(id);
        image.setType(ImageType.MAIN_IMAGE);
        image.setUrl("https://example.com/main.jpg");
        product.setImages(List.of(image));
        product.setSkus(List.of());
        return product;
    }

    private static ProductSKU sku(
            Long id,
            Long productId,
            Integer status,
            Integer productStatus
    ) {
        ProductSKU sku = new ProductSKU();
        sku.setId(id);
        sku.setProductId(productId);
        sku.setStatus(status);
        sku.setProductStatus(productStatus);
        sku.setSkuCode("SKU-" + id);
        sku.setPrice(9900L);
        return sku;
    }

    private static Stream<List<Long>> invalidSkuIdLists() {
        return Stream.of(
                Arrays.asList(1001L, null),
                List.of(1001L, MIN_VALID_ID - 1),
                List.of(1001L, MIN_VALID_ID - 2)
        );
    }

    private static void assertBusinessError(
            BusinessException exception,
            String expectedCode,
            String expectedMessage,
            int expectedHttpStatus
    ) {
        assertEquals(expectedCode, exception.getCode());
        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(expectedHttpStatus, exception.getHttpStatus());
    }
}
