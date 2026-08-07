package io.github.sakana.stock.pojo.vo;

import io.github.sakana.stock.enumeration.StockStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockVO {

    private StockStatus status;
}
