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
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static io.github.sakana.product.constant.ProductConstants.DEFAULT_PAGE_NUMBER;
import static io.github.sakana.product.constant.ProductConstants.DEFAULT_PAGE_SIZE;
import static io.github.sakana.product.constant.ProductConstants.MIN_VALID_ID;

class ProductControllerTest {

    private final StubProductService productService = new StubProductService();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        productService.failure = null;
        productService.product = null;
        productService.pageResult = null;

        ProductController controller = new ProductController();
        ReflectionTestUtils.setField(controller, "productService", productService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("分页请求体为空时返回统一的400请求错误")
    void shouldRejectNullPageBody() throws Exception {
        mockMvc.perform(post("/api/product/page")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_INVALID"))
                .andExpect(jsonPath("$.msg").value("请求参数格式错误"));
    }

    @Test
    @DisplayName("商品分类ID不合法时返回统一的400业务错误")
    void shouldReturnInvalidCategoryIdError() throws Exception {
        productService.failure = ProductErrorCode.CATEGORY_ID_INVALID.exception();

        mockMvc.perform(post("/api/product/page")
                .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"page\":" + DEFAULT_PAGE_NUMBER
                                + ",\"size\":" + DEFAULT_PAGE_SIZE
                                + ",\"categoryId\":" + (MIN_VALID_ID - 1) + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PRODUCT_CATEGORY_ID_INVALID"))
                .andExpect(jsonPath("$.msg").value("商品分类ID不合法"));
    }

    @Test
    @DisplayName("分页查询成功时返回SUCCESS")
    void shouldReturnProductPage() throws Exception {
        productService.pageResult = PageVO.<ProductVO>builder()
                .items(List.of())
                .total(0L)
                .page(DEFAULT_PAGE_NUMBER)
                .size(DEFAULT_PAGE_SIZE)
                .pages(0L)
                .build();

        mockMvc.perform(post("/api/product/page")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"page\":" + DEFAULT_PAGE_NUMBER
                                + ",\"size\":" + DEFAULT_PAGE_SIZE + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.page").value(DEFAULT_PAGE_NUMBER))
                .andExpect(jsonPath("$.data.size").value(DEFAULT_PAGE_SIZE));
    }

    @Test
    @DisplayName("商品ID不合法时返回统一的400业务错误")
    void shouldReturnInvalidProductIdError() throws Exception {
        productService.failure = ProductErrorCode.PRODUCT_ID_INVALID.exception();

        mockMvc.perform(get("/api/product/{id}", MIN_VALID_ID - 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PRODUCT_ID_INVALID"))
                .andExpect(jsonPath("$.msg").value("商品ID不合法"));
    }

    @Test
    @DisplayName("商品不存在时返回统一的404业务错误")
    void shouldReturnProductNotFoundError() throws Exception {
        productService.failure = ProductErrorCode.PRODUCT_NOT_FOUND.exception(
                Map.of("productId", 1001L)
        );

        mockMvc.perform(get("/api/product/1001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.msg").value("商品不存在"))
                .andExpect(jsonPath("$.data.productId").value(1001));
    }

    @Test
    @DisplayName("商品已下架时返回统一的409业务错误")
    void shouldReturnProductNotOnSaleError() throws Exception {
        productService.failure = ProductErrorCode.PRODUCT_NOT_ON_SALE.exception(
                Map.of("productId", 1001L)
        );

        mockMvc.perform(get("/api/product/1001"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_ON_SALE"))
                .andExpect(jsonPath("$.msg").value("商品已下架"))
                .andExpect(jsonPath("$.data.productId").value(1001));
    }

    @Test
    @DisplayName("商品详情查询成功时返回SUCCESS")
    void shouldReturnProductDetail() throws Exception {
        Product product = new Product();
        product.setId(1001L);
        product.setName("测试商品");
        product.setStatus(OnSaleType.ONSALE);
        product.setImages(List.of());
        ProductSKU sku = new ProductSKU();
        sku.setPrice(9900L);
        product.setSkus(List.of(sku));
        productService.product = product;

        mockMvc.perform(get("/api/product/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1001))
                .andExpect(jsonPath("$.data.name").value("测试商品"))
                .andExpect(jsonPath("$.data.minPrice").value(9900));
    }

    private static class StubProductService implements ProductService {

        private Product product;
        private PageVO<ProductVO> pageResult;
        private BusinessException failure;

        @Override
        public PageVO<ProductVO> page(ProductPageDTO pageDTO) {
            if (failure != null) {
                throw failure;
            }
            return pageResult;
        }

        @Override
        public Product getDetail(Long id) {
            if (failure != null) {
                throw failure;
            }
            return product;
        }

        @Override
        public List<ProductSKU> getSkuTradeInfo(List<Long> skuIds) {
            throw new UnsupportedOperationException();
        }
    }
}
