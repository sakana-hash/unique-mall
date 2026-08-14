package io.github.sakana.api.client;

import io.github.sakana.api.pojo.dto.SkuTradeDTO;
import io.github.sakana.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("product-service")
public interface ProductClient {

    @PostMapping("/internal/product/sku/trade-info")
    Result<List<SkuTradeDTO>> getSkuTradeInfo(@RequestBody List<Long> skuIds);
}
