package io.github.sakana.product.service.impl;

import io.github.sakana.common.exception.BusinessException;
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
    @ValueSource(longs = {0, -1})
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
        request.setPage(0);
        request.setSize(101);
        request.setSort(PageSort.DEFAULT);
        PageResult cachedResult = PageResult.builder()
                .ids(List.of())
                .total(0L)
                .build();
        when(cacheService.buildPageResultKey(1, 100, null, PageSort.DEFAULT))
                .thenReturn("page-key");
        when(cacheService.getPageResult("page-key")).thenReturn(cachedResult);

        PageVO<ProductVO> result = productService.page(request);

        assertEquals(1, result.getPage());
        assertEquals(100, result.getSize());
        assertEquals(0L, result.getTotal());
        assertEquals(0L, result.getPages());
        assertEquals(List.of(), result.getItems());
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
    @DisplayName("SKU查询数量超过50时返回400并携带数量限制")
    void shouldRejectTooManySkuIds() {
        List<Long> skuIds = LongStream.rangeClosed(1, 51).boxed().toList();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.getSkuTradeInfo(skuIds)
        );

        assertBusinessError(
                exception,
                "PRODUCT_SKU_QUERY_LIMIT_EXCEEDED",
                "单次最多查询50个SKU",
                400
        );
        assertEquals(Map.of("currentCount", 51, "maxCount", 50), exception.getDetails());
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
        ProductSKU existingSku = sku(1001L, 2001L, 1, 1);
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
        ProductSKU unavailableSku = sku(1001L, 2001L, 0, 1);
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
        ProductSKU sku = sku(1001L, 2001L, 1, 1);
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
    @ValueSource(longs = {0, -1})
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
        Product product = product(1001L, 0);
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
        when(cacheService.getProduct(1001L)).thenReturn(product(1001L, 0));

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
        Product cachedProduct = product(1001L, 1);
        when(cacheService.getProduct(1001L)).thenReturn(cachedProduct);

        Product result = productService.getDetail(1001L);

        assertSame(cachedProduct, result);
        verifyNoInteractions(productMapper, detailMapper, imageMapper, skuMapper);
    }

    @Test
    @DisplayName("缓存未命中时组装商品详情并回写缓存")
    void shouldAssembleAndCacheProduct() {
        Product product = product(1001L, 1);
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
        Product product = product(id, 1);
        ProductImage image = new ProductImage();
        image.setProductId(id);
        image.setType(1);
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
                List.of(1001L, 0L),
                List.of(1001L, -1L)
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
