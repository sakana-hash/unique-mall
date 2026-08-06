package io.github.sakana.product.controller;

import io.github.sakana.common.result.Result;
import io.github.sakana.product.pojo.dto.ProductPageDTO;
import io.github.sakana.product.pojo.entity.Product;
import io.github.sakana.product.pojo.vo.PageVO;
import io.github.sakana.product.pojo.vo.ProductDetailVO;
import io.github.sakana.product.pojo.vo.ProductVO;
import io.github.sakana.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping("/page")
    public Result<PageVO<ProductVO>> page(@RequestBody ProductPageDTO pageDTO) {
        PageVO<ProductVO> pageVO = productService.page(pageDTO);
        return Result.success(pageVO);
    }

    @GetMapping("/{id}")
    public Result<ProductDetailVO> get(@PathVariable("id") Long id) {
        Product product = productService.getDetail(id);
        return Result.success(product.toDetailVO());
    }
}
