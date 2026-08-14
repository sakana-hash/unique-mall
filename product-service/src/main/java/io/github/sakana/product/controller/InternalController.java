package io.github.sakana.product.controller;

import io.github.sakana.api.pojo.dto.SkuTradeDTO;
import io.github.sakana.common.result.Result;
import io.github.sakana.product.pojo.entity.ProductSKU;
import io.github.sakana.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/product")
public class InternalController {

    @Autowired
    private ProductService productService;

    @PostMapping("/sku/trade-info")
    public Result<List<SkuTradeDTO>> getSkuTradeInfo(@RequestBody List<Long> skuIds) {
        List<ProductSKU> skus = productService.getSkuTradeInfo(skuIds);
        List<SkuTradeDTO> skuTradeDTOs = skus.stream()
                .map(sku -> SkuTradeDTO.builder()
                        .skuId(sku.getId())
                        .productId(sku.getProductId())
                        .skuCode(sku.getSkuCode())
                        .productName(sku.getProductName())
                        .imageUrl(sku.getImageUrl())
                        .price(sku.getPrice())
                        .skuStatus(sku.getStatus())
                        .productStatus(sku.getProductStatus())
                        .build())
                .toList();
        return Result.success(skuTradeDTOs);
    }
}
