package io.github.sakana.product.service.impl;

import io.github.sakana.product.enumeration.PageSort;
import io.github.sakana.product.mapper.ProductDetailMapper;
import io.github.sakana.product.mapper.ProductImageMapper;
import io.github.sakana.product.mapper.ProductMapper;
import io.github.sakana.product.mapper.ProductSKUMapper;
import io.github.sakana.product.pojo.PageQuery;
import io.github.sakana.product.pojo.dto.ProductPageDTO;
import io.github.sakana.product.pojo.entity.*;
import io.github.sakana.product.pojo.vo.PageVO;
import io.github.sakana.product.pojo.vo.ProductVO;
import io.github.sakana.product.service.CacheService;
import io.github.sakana.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductSKUMapper skuMapper;
    @Autowired
    private ProductDetailMapper detailMapper;
    @Autowired
    private ProductImageMapper imageMapper;
    @Autowired
    private CacheService cacheService;


    @Override
    public PageVO<ProductVO> page(ProductPageDTO pageDTO) {
        Integer page = pageDTO.getPage();
        Integer size = pageDTO.getSize();
        Long categoryId = pageDTO.getCategoryId();
        PageSort sort = pageDTO.getSort();
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 1;
        } else if (size > 100) {
            size = 100;
        }
        if (categoryId != null && categoryId <= 0) {
            throw new RuntimeException(String.format(
                    "无效的categoryId: %d", categoryId
            ));
        }

        String key = cacheService.buildPageResultKey(page, size, categoryId, sort);
        PageResult result = cacheService.getPageResult(key);
        if (result == null) {
            PageQuery query = new PageQuery((page - 1) * size, size, categoryId, sort);
            result = new PageResult(productMapper.selectPage(query), productMapper.count(query));
            cacheService.setPageResult(key, result);
        }

        List<Product> products = batchGetDetails(result.getIds());
        List<ProductVO> productVOS = products.stream()
                .map(Product::toPageVO)
                .collect(Collectors.toList());

        Long pages = (result.getTotal() + size - 1) / size; // 总页数
        return new PageVO<>(productVOS, result.getTotal(), page, size, pages);
    }

    @Override
    public Product getDetail(Long id) {
        if (id == null || id <= 0) {
            throw new RuntimeException(String.format(
                    "无效的商品id: %d", id
            ));
        }

        Product product = cacheService.getProduct(id);
        if (product != null) {
            return product;
        }

        product = productMapper.selectById(id);
        if (product == null || product.getStatus() != 1) {
            throw new RuntimeException(String.format(
                    "商品不存在: %d", id
            ));
        }

        ProductDetail detail = detailMapper.selectByProductId(id);
        if (detail != null) {
            product.setContent(detail.getContent());
        }
        product.setImages(imageMapper.selectByProductId(id));
        product.setSkus(skuMapper.selectByProductId(id));

        cacheService.setProduct(product);
        return product;
    }

    /**
     * 根据 id 批量获取商品明细：优先从 Redis 缓存读取，未命中的 id 再查询数据库并重建缓存，
     * 最终按输入 ids 的顺序合并返回；Redis 异常时降级为纯数据库查询。
     *
     * @param ids 商品 id 列表
     * @return 商品明细列表（与入参 ids 顺序一致）
     */
    private List<Product> batchGetDetails(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }


        Map<Long, Product> cachedMap = new HashMap<>();
        List<Long> missIds = new ArrayList<>();

        List<Product> products = cacheService.getProducts(ids);
        if (products == null) {
            missIds = ids;
        } else {
            for (int i = 0; i < ids.size(); i++) {
                Long id = ids.get(i);
                Product product = products.get(i);
                if (product == null) {
                    missIds.add(id);
                } else {
                    cachedMap.put(id, product);
                }
            }
        }

        // 2. 未命中的 id 去数据库查询并组装
        if (!missIds.isEmpty()) {
            List<Product> dbProducts = queryAndAssemble(missIds);

            // 3. 将未命中的 products 写回 redis 缓存
            for (Product product : dbProducts) {
                cachedMap.put(product.getId(), product);
                cacheService.setProduct(product);
            }
        }

        // 4. 合并缓存与数据库结果，并按输入 ids 的顺序返回
        List<Product> result = new ArrayList<>();
        for (Long id : ids) {
            Product product = cachedMap.get(id);
            if (product != null) {
                result.add(product);
            }
        }
        return result;
    }

    /**
     * 从数据库批量查询商品基础信息、图片、SKU、详情并组装为商品明细
     */
    private List<Product> queryAndAssemble(List<Long> ids) {
        List<Product> products = productMapper.selectByIds(ids);
        if (products.isEmpty()) {
            return products;
        }
        List<ProductImage> productImages = imageMapper.selectByProductIds(ids);
        List<ProductSKU> productSKUS = skuMapper.selectByProductIds(ids);
        List<ProductDetail> productDetails = detailMapper.selectByProductIds(ids);

        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, List<ProductImage>> imageMap = productImages.stream()
                .collect(Collectors.groupingBy(ProductImage::getProductId));
        Map<Long, List<ProductSKU>> skuMap = productSKUS.stream()
                .collect(Collectors.groupingBy(ProductSKU::getProductId));
        Map<Long, ProductDetail> detailMap = productDetails.stream()
                .collect(Collectors.toMap(
                        ProductDetail::getProductId,
                        Function.identity()
                ));

        List<Product> result = new ArrayList<>();
        for (Long id : ids) {
            Product product = productMap.get(id);
            if (product != null) {
                product.setImages(imageMap.getOrDefault(id, Collections.emptyList()));
                product.setSkus(skuMap.getOrDefault(id, Collections.emptyList()));
                product.setContent(detailMap.get(id) != null
                        ? detailMap.get(id).getContent()
                        : null);
                result.add(product);
            }
        }
        return result;
    }

}
