package io.github.sakana.product.mapper;

import io.github.sakana.product.pojo.entity.ProductImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductImageMapper {

    List<ProductImage> selectByProductIds(@Param("ids") List<Long> ids);
}
