package io.github.sakana.product.mapper;

import io.github.sakana.product.pojo.entity.ProductDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductDetailMapper {

    List<ProductDetail> selectByProductIds(@Param("ids") List<Long> ids);
}
