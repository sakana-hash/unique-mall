package io.github.sakana.product.service.impl;

import io.github.sakana.common.exception.BusinessException;
import io.github.sakana.product.mapper.ProductDetailMapper;
import io.github.sakana.product.mapper.ProductImageMapper;
import io.github.sakana.product.mapper.ProductMapper;
import io.github.sakana.product.mapper.ProductSKUMapper;
import io.github.sakana.product.pojo.entity.Product;
import io.github.sakana.product.pojo.entity.ProductDetail;
import io.github.sakana.product.pojo.entity.ProductImage;
import io.github.sakana.product.pojo.entity.ProductSKU;
import io.github.sakana.product.service.CacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

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
