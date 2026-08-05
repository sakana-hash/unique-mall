package io.github.sakana.product.service.impl;

import io.github.sakana.product.mapper.ProductDetailMapper;
import io.github.sakana.product.mapper.ProductImageMapper;
import io.github.sakana.product.mapper.ProductMapper;
import io.github.sakana.product.mapper.ProductSKUMapper;
import io.github.sakana.product.pojo.ProductPageQuery;
import io.github.sakana.product.pojo.dto.ProductPageDTO;
import io.github.sakana.product.pojo.entity.Product;
import io.github.sakana.product.pojo.entity.ProductDetail;
import io.github.sakana.product.pojo.entity.ProductImage;
import io.github.sakana.product.pojo.entity.ProductSKU;
import io.github.sakana.product.pojo.vo.PageVO;
import io.github.sakana.product.pojo.vo.ProductPageVO;
import io.github.sakana.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
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
    private StringRedisTemplate redisTemplate;

    @Override
    public PageVO<ProductPageVO> page(ProductPageDTO pageDTO) {
        Integer page = pageDTO.getPage();
        Integer size = pageDTO.getSize();
        Long categoryId = pageDTO.getCategoryId();
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

        ProductPageQuery query = new ProductPageQuery();
        query.setOffset((page - 1) * size);
        query.setSize(size);
        query.setCategoryId(pageDTO.getCategoryId());
        query.setSort(pageDTO.getSort());

        List<Long> ids = productMapper.selectPage(query);
        Long total = productMapper.count(query);

        List<Product> products = batchGetDetails(ids);
        List<ProductPageVO> productPageVOS = products.stream()
                .map(Product::toPageVO)
                .collect(Collectors.toList());
        PageVO<ProductPageVO> pageVO = new PageVO<>();
        pageVO.setItems(productPageVOS);
        pageVO.setTotal(total);
        pageVO.setPage(page);
        pageVO.setSize(size);
        pageVO.setPages((total + size - 1) / size);
        return pageVO;
    }

    private List<Product> batchGetDetails(List<Long> ids) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        List<Product> products = productMapper.selectByIds(ids);
        if (products.isEmpty()) {
            return new ArrayList<>();
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
