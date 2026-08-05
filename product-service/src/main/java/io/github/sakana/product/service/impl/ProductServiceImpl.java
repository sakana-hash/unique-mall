package io.github.sakana.product.service.impl;

import io.github.sakana.product.mapper.ProductMapper;
import io.github.sakana.product.pojo.ProductPageQuery;
import io.github.sakana.product.pojo.dto.ProductPageDTO;
import io.github.sakana.product.pojo.vo.PageVO;
import io.github.sakana.product.pojo.vo.ProductPageVO;
import io.github.sakana.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

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

        List<ProductPageVO> productPageVOList = productMapper.selectPage(query);
        Long total = productMapper.count(query);

        PageVO<ProductPageVO> pageVO = new PageVO<>();
        pageVO.setItems(productPageVOList);
        pageVO.setTotal(total);
        pageVO.setPage(page);
        pageVO.setSize(size);
        pageVO.setPages((total + size - 1) / size);
        return pageVO;
    }
}
