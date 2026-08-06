package io.github.sakana.product.mapper;

import io.github.sakana.product.pojo.PageQuery;
import io.github.sakana.product.pojo.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductMapper {

    List<Long> selectPage(PageQuery query);

    Long count(PageQuery query);

    List<Product> selectByIds(@Param("ids") List<Long> ids);

    @Select("select * from product.product where id = #{id}")
    Product selectById(@Param("id") Long id);
}
