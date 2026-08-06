package io.github.sakana.mockdata.mapper;

import io.github.sakana.mockdata.pojo.ProductCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * product_category 批量插入 Mapper
 */
@Mapper
public interface MockProductCategoryMapper {

    /**
     * 多行 VALUES 批量插入，一次调用的行数即 batch size
     */
    int batchInsert(@Param("list") List<ProductCategory> list);

    void truncate();
}
