package io.github.sakana.product.mapper;

import io.github.sakana.product.pojo.PageQuery;
import io.github.sakana.product.pojo.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {

    List<Long> selectPage(PageQuery query);

    Long count(PageQuery query);

    List<Product> selectByIds(@Param("ids") List<Long> ids);
}
