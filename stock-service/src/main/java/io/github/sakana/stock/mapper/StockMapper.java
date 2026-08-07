package io.github.sakana.stock.mapper;

import io.github.sakana.stock.pojo.entity.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StockMapper {

    List<Stock> selectBySkuIds(@Param("skuIds") List<Long> skuIds);
}
