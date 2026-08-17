package io.github.sakana.product.controller;

import io.github.sakana.common.exception.BusinessException;
import io.github.sakana.exception.GlobalExceptionHandler;
import io.github.sakana.product.constant.OnSaleType;
import io.github.sakana.product.enumeration.ProductErrorCode;
import io.github.sakana.product.pojo.dto.ProductPageDTO;
import io.github.sakana.product.pojo.entity.Product;
import io.github.sakana.product.pojo.entity.ProductSKU;
import io.github.sakana.product.pojo.vo.PageVO;
import io.github.sakana.product.pojo.vo.ProductVO;
import io.github.sakana.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalControllerTest {

    private final StubProductService productService = new StubProductService();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        productService.failure = null;
        productService.skus = List.of();

        InternalController controller = new InternalController();
        ReflectionTestUtils.setField(controller, "productService", productService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("SKU查询请求体为空时返回统一的400请求错误")
    void shouldRejectNullRequestBody() throws Exception {
        mockMvc.perform(post("/internal/product/sku/trade-info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_INVALID"))
                .andExpect(jsonPath("$.msg").value("请求参数格式错误"));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("businessErrors")
    @DisplayName("SKU查询业务错误返回对应HTTP状态和错误码")
    void shouldReturnBusinessError(
            ProductErrorCode errorCode,
            String expectedCode,
            int expectedStatus
    ) throws Exception {
        productService.failure = errorCode.exception();

        mockMvc.perform(post("/internal/product/sku/trade-info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1001]"))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.code").value(expectedCode))
                .andExpect(jsonPath("$.msg").value(errorCode.getMessage()));
    }

    @Test
    @DisplayName("SKU交易信息查询成功时返回SUCCESS和DTO数据")
    void shouldReturnSkuTradeInfo() throws Exception {
        ProductSKU sku = new ProductSKU();
        sku.setId(1001L);
        sku.setProductId(2001L);
        sku.setSkuCode("SKU-1001");
        sku.setProductName("测试商品");
        sku.setImageUrl("https://example.com/main.jpg");
        sku.setPrice(9900L);
        sku.setStatus(OnSaleType.ONSALE);
        sku.setProductStatus(OnSaleType.ONSALE);
        productService.skus = List.of(sku);

        mockMvc.perform(post("/internal/product/sku/trade-info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1001]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].skuId").value(1001))
                .andExpect(jsonPath("$.data[0].productId").value(2001))
                .andExpect(jsonPath("$.data[0].productName").value("测试商品"))
                .andExpect(jsonPath("$.data[0].price").value(9900));
    }

    private static Stream<Arguments> businessErrors() {
        return Stream.of(
                Arguments.of(ProductErrorCode.SKU_IDS_REQUIRED,
                        "PRODUCT_SKU_IDS_REQUIRED", 400),
                Arguments.of(ProductErrorCode.SKU_QUERY_LIMIT_EXCEEDED,
                        "PRODUCT_SKU_QUERY_LIMIT_EXCEEDED", 400),
                Arguments.of(ProductErrorCode.SKU_ID_INVALID,
                        "PRODUCT_SKU_ID_INVALID", 400),
                Arguments.of(ProductErrorCode.SKU_ID_DUPLICATED,
                        "PRODUCT_SKU_ID_DUPLICATED", 400),
                Arguments.of(ProductErrorCode.SKU_NOT_FOUND,
                        "PRODUCT_SKU_NOT_FOUND", 404),
                Arguments.of(ProductErrorCode.SKU_NOT_AVAILABLE,
                        "PRODUCT_SKU_NOT_AVAILABLE", 409)
        );
    }

    private static class StubProductService implements ProductService {

        private List<ProductSKU> skus;
        private BusinessException failure;

        @Override
        public PageVO<ProductVO> page(ProductPageDTO pageDTO) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Product getDetail(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ProductSKU> getSkuTradeInfo(List<Long> skuIds) {
            if (failure != null) {
                throw failure;
            }
            return skus;
        }
    }
}
