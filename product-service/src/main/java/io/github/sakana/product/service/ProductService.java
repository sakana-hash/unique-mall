package io.github.sakana.product.service;

import io.github.sakana.product.pojo.dto.ProductPageDTO;
import io.github.sakana.product.pojo.entity.Product;
import io.github.sakana.product.pojo.vo.PageVO;
import io.github.sakana.product.pojo.vo.ProductVO;

public interface ProductService {

    PageVO<ProductVO> page(ProductPageDTO pageDTO);

    Product getDetail(Long id);
}
