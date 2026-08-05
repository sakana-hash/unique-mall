package io.github.sakana.product.mapper;

import io.github.sakana.product.pojo.ProductPageQuery;
import io.github.sakana.product.pojo.vo.ProductPageVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProductMapper {

    List<ProductPageVO> selectPage(ProductPageQuery query);

    Long count(ProductPageQuery query);
}
