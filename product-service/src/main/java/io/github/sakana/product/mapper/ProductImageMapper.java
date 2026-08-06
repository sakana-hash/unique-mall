package io.github.sakana.product.mapper;

import io.github.sakana.product.pojo.entity.ProductImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductImageMapper {

    List<ProductImage> selectByProductIds(@Param("ids") List<Long> ids);

    @Select("select * from product.product_image where product_id = #{id}")
    List<ProductImage> selectByProductId(@Param("id") Long id);
}
