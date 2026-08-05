package io.github.sakana.product.mapper;

import io.github.sakana.product.pojo.entity.ProductSKU;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductSKUMapper {

    List<ProductSKU> selectByProductIds(@Param("ids") List<Long> ids);
}
