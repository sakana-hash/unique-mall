package io.github.sakana.product.service;

import io.github.sakana.product.pojo.dto.ProductPageDTO;
import io.github.sakana.product.pojo.vo.PageVO;
import io.github.sakana.product.pojo.vo.ProductPageVO;

public interface ProductService {

    PageVO<ProductPageVO> page(ProductPageDTO pageDTO);
}
