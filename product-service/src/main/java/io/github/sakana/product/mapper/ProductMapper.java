package io.github.sakana.product.mapper;

import io.github.sakana.product.pojo.ProductPageQuery;
import io.github.sakana.product.pojo.entity.Product;
import io.github.sakana.product.pojo.vo.ProductPageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {

    List<Long> selectPage(ProductPageQuery query);

    Long count(ProductPageQuery query);

    List<Product> selectByIds(@Param("ids") List<Long> ids);
}
