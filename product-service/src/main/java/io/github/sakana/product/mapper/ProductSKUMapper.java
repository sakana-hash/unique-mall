package io.github.sakana.product.mapper;

import io.github.sakana.product.pojo.entity.ProductSKU;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductSKUMapper {

    List<ProductSKU> selectByProductIds(@Param("ids") List<Long> ids);

    @Select("select * from product.product_sku where product_id = #{id}")
    List<ProductSKU> selectByProductId(@Param("id") Long id);

    List<ProductSKU> selectByIds(@Param("ids") List<Long> ids);
}
