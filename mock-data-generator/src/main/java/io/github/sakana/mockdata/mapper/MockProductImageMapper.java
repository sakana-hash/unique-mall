package io.github.sakana.mockdata.mapper;

import io.github.sakana.product.pojo.entity.ProductImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * product_image 批量插入 Mapper
 */
@Mapper
public interface MockProductImageMapper {

    /**
     * 多行 VALUES 批量插入，一次调用的行数即 batch size
     */
    int batchInsert(@Param("list") List<ProductImage> list);

    void truncate();
}
