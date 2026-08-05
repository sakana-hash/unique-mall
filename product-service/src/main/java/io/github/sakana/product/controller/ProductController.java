package io.github.sakana.product.controller;

import io.github.sakana.common.result.Result;
import io.github.sakana.product.pojo.dto.ProductPageDTO;
import io.github.sakana.product.pojo.vo.PageVO;
import io.github.sakana.product.pojo.vo.ProductPageVO;
import io.github.sakana.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping("/page")
    public Result<PageVO<ProductPageVO>> page(@RequestBody ProductPageDTO pageDTO) {
        PageVO<ProductPageVO> pageVO = productService.page(pageDTO);
        return Result.success(pageVO);
    }
}
