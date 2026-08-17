package io.github.sakana.product.service.impl;

import io.github.sakana.product.constant.ImageType;
import io.github.sakana.product.constant.OnSaleType;
import io.github.sakana.product.enumeration.PageSort;
import io.github.sakana.product.enumeration.ProductErrorCode;
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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.github.sakana.product.constant.ProductConstants.DEFAULT_PAGE_NUMBER;
import static io.github.sakana.product.constant.ProductConstants.MAX_PAGE_SIZE;
import static io.github.sakana.product.constant.ProductConstants.MAX_SKU_QUERY_COUNT;
import static io.github.sakana.product.constant.ProductConstants.MIN_PAGE_NUMBER;
import static io.github.sakana.product.constant.ProductConstants.MIN_PAGE_SIZE;
import static io.github.sakana.product.constant.ProductConstants.MIN_VALID_ID;
import static io.github.sakana.product.constant.ProductConstants.PAGE_CACHE_COLD_START_LOCK_WAIT_SECONDS;

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
    private CacheService cacheService;
    @Autowired
    private RedissonClient redissonClient;


    @Override
    public PageVO<ProductVO> page(ProductPageDTO pageDTO) {
        // 解析参数并进行校验
        if (pageDTO == null) {
            throw ProductErrorCode.PAGE_REQUEST_REQUIRED.exception();
        }

        Integer page = pageDTO.getPage();
        Integer size = pageDTO.getSize();
        Long categoryId = pageDTO.getCategoryId();
        PageSort sort = pageDTO.getSort();
        if (page == null || page < MIN_PAGE_NUMBER) {
            page = DEFAULT_PAGE_NUMBER;
        }
        if (size == null || size < MIN_PAGE_SIZE) {
            size = MIN_PAGE_SIZE;
        } else if (size > MAX_PAGE_SIZE) {
            size = MAX_PAGE_SIZE;
        }
        if (categoryId != null && categoryId < MIN_VALID_ID) {
            throw ProductErrorCode.CATEGORY_ID_INVALID.exception();
        }

        PageResult result = getPageResult(page, size, categoryId, sort);

        // 获取分页结果后，根据商品id列表批量查询商品详情
        List<Product> products = batchGetDetails(result.getIds());
        List<ProductVO> productVOS = products.stream()
                .map(Product::toPageVO)
                .collect(Collectors.toList());

        // 组装查询结果并返回
        Long pages = (result.getTotal() + size - MIN_PAGE_NUMBER) / size; // 总页数
        return PageVO.<ProductVO>builder()
                .items(productVOS)
                .total(result.getTotal())
                .page(page)
                .size(size)
                .pages(pages)
                .build();
    }

    private PageResult getPageResult(Integer page, Integer size, Long categoryId, PageSort sort) {
        // 尝试先从缓存服务中获取分页结果
        String key = cacheService.buildPageResultKey(page, size, categoryId, sort);
        PageResult result = cacheService.getPageResult(key);

        // 如果获取到了未逻辑过期的数据，直接返回
        if (isFreshPageResult(result)) {
            return result;
        }

        // 获取到了逻辑过期的数据，抢到锁的线程进行重建，其他线程直接返回旧数据
        if (result != null) {
            return rebuildExpiredPageResult(
                    key, page, size, categoryId, sort, result
            );
        }

        // 没有可用的旧数据时，降级为抢到锁的线程进行重建，其他线程等待锁
        return rebuildColdPageResult(key, page, size, categoryId, sort);
    }

    private PageResult rebuildExpiredPageResult(
            String key,
            Integer page,
            Integer size,
            Long categoryId,
            PageSort sort,
            PageResult staleResult
    ) {
        RLock lock = redissonClient.getLock(buildPageCacheLockKey(key));
        boolean locked = false;

        try {
            locked = lock.tryLock();
            if (!locked) {
                return staleResult;
            }

            PageResult latest = cacheService.getPageResult(key);
            if (isFreshPageResult(latest)) {
                return latest;
            }
            return rebuildPageResult(key, page, size, categoryId, sort);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private PageResult rebuildColdPageResult(
            String key,
            Integer page,
            Integer size,
            Long categoryId,
            PageSort sort
    ) {
        RLock lock = redissonClient.getLock(buildPageCacheLockKey(key));
        boolean locked = false;
        try {
            locked = lock.tryLock(
                    PAGE_CACHE_COLD_START_LOCK_WAIT_SECONDS,
                    TimeUnit.SECONDS
            );
            if (!locked) {
                PageResult latest = cacheService.getPageResult(key);
                if (latest != null) {
                    return latest;
                }
                throw new IllegalStateException("等待分页缓存初始化超时");
            }

            PageResult latest = cacheService.getPageResult(key);
            if (latest != null) {
                return latest;
            }

            return rebuildPageResult(key, page, size, categoryId, sort);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待分页缓存初始化时线程被中断", e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private PageResult rebuildPageResult(
            String key,
            Integer page,
            Integer size,
            Long categoryId,
            PageSort sort
    ) {
        PageQuery query = PageQuery.builder()
                .offset((page - MIN_PAGE_NUMBER) * size)
                .size(size)
                .categoryId(categoryId)
                .sort(sort)
                .build();
        PageResult result = PageResult.builder()
                .ids(productMapper.selectPage(query))
                .total(productMapper.count(query))
                .build();
        cacheService.setPageResult(key, result);
        return result;
    }

    private static String buildPageCacheLockKey(String key) {
        return "lock:" + key;
    }

    private static boolean isFreshPageResult(PageResult result) {
        return result != null && LocalDateTime.now().isBefore(result.getExpireTime());
    }

    @Override
    public Product getDetail(Long id) {
        // id合法性校验
        if (id == null || id < MIN_VALID_ID) {
            throw ProductErrorCode.PRODUCT_ID_INVALID.exception();
        }

        // 尝试先从缓存服务中获取商品详情
        Product product = cacheService.getProduct(id);
        if (product != null) {
            // 获取到商品详情后，对商品进行在售检验
            if (!Objects.equals(product.getStatus(), OnSaleType.ONSALE)) {
                throw ProductErrorCode.PRODUCT_NOT_ON_SALE.exception(Map.of("productId", id));
            }
            return product;
        }

        // 没有命中缓存时，降级到数据库查找
        product = productMapper.selectById(id);
        if (product == null) {
            // info 不存在 ID 的数据库查询约 20ms, 成本可接受, 如果攻击者使用同一用户进行攻击，考虑网关用户级限流
            throw ProductErrorCode.PRODUCT_NOT_FOUND.exception(Map.of("productId", id));
        }
        if (!Objects.equals(product.getStatus(), OnSaleType.ONSALE)) {
            throw ProductErrorCode.PRODUCT_NOT_ON_SALE.exception(Map.of("productId", id));
        }

        // 组装商品详情
        ProductDetail detail = detailMapper.selectByProductId(id);
        if (detail != null) {
            product.setContent(detail.getContent());
        }
        product.setImages(imageMapper.selectByProductId(id));
        product.setSkus(skuMapper.selectByProductId(id));

        // 将商品详情写入缓存
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
        // 非空性校验
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }


        Map<Long, Product> cachedMap = new HashMap<>();
        List<Long> missIds = new ArrayList<>();

        // 先尝试从缓存中获取商品详情，记录未命中缓存的商品id
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

    public List<ProductSKU> getSkuTradeInfo(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            throw ProductErrorCode.SKU_IDS_REQUIRED.exception();
        }
        if (skuIds.size() > MAX_SKU_QUERY_COUNT) {
            throw ProductErrorCode.SKU_QUERY_LIMIT_EXCEEDED.exception(Map.of(
                    "currentCount", skuIds.size(),
                    "maxCount", MAX_SKU_QUERY_COUNT
            ));
        }

        Set<Long> uniqueSkuIds = new HashSet<>();
        for (int index = 0; index < skuIds.size(); index++) {
            Long skuId = skuIds.get(index);
            if (skuId == null || skuId < MIN_VALID_ID) {
                throw ProductErrorCode.SKU_ID_INVALID.exception(Map.of("index", index));
            }
            if (!uniqueSkuIds.add(skuId)) {
                throw ProductErrorCode.SKU_ID_DUPLICATED.exception(Map.of("skuId", skuId));
            }
        }

        List<ProductSKU> productSKUS = skuMapper.selectByIds(skuIds);
        Set<Long> foundSkuIds = productSKUS.stream()
                .map(ProductSKU::getId)
                .collect(Collectors.toSet());
        List<Long> missingSkuIds = skuIds.stream()
                .filter(skuId -> !foundSkuIds.contains(skuId))
                .toList();
        if (!missingSkuIds.isEmpty()) {
            throw ProductErrorCode.SKU_NOT_FOUND.exception(Map.of("skuIds", missingSkuIds));
        }

        List<Long> unavailableSkuIds = productSKUS.stream()
                .filter(sku -> !OnSaleType.ONSALE.equals(sku.getProductStatus())
                        || !OnSaleType.ONSALE.equals(sku.getStatus()))
                .map(ProductSKU::getId)
                .toList();
        if (!unavailableSkuIds.isEmpty()) {
            throw ProductErrorCode.SKU_NOT_AVAILABLE.exception(Map.of(
                    "skuIds", unavailableSkuIds
            ));
        }

        List<Long> productIds = productSKUS.stream()
                .map(ProductSKU::getProductId)
                .distinct()
                .toList();
        List<Product> products = batchGetDetails(productIds);
        Map<Long, Product> productMap = products.stream().collect(Collectors.toMap(Product::getId, Function.identity()));

        productSKUS = productSKUS.stream().map(sku -> {
            Product product = productMap.get(sku.getProductId());
            if (product == null) {
                throw new IllegalStateException(
                        "不合法的数据库状态: sku关联的商品不存在, skuId: " + sku.getId()
                );
            }

            String mainImageUrl = Optional.ofNullable(product.getImages())
                    .orElseGet(Collections::emptyList)
                    .stream()
                    .filter(image -> ImageType.MAIN_IMAGE.equals(image.getType()))
                    .map(ProductImage::getUrl)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(String.format(
                            "不合法的数据库状态: 商品主图不存在, productId: %d, skuId: %d",
                            product.getId(), sku.getId()
                    )));

            sku.setProductName(product.getName());
            sku.setImageUrl(mainImageUrl);
            return sku;
        }).toList();

        return productSKUS;
    }
}
