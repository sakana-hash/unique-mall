package io.github.sakana.stock.enumeration;

public enum StockStatus {

    IN_STOCK,
    LOW_STOCK,
    OUT_OF_STOCK;

    public static StockStatus from(Integer available) {
        if (available == null || available <= 0) {
            return OUT_OF_STOCK;
        }

        if (available <= 10) {
            return LOW_STOCK;
        }

        return IN_STOCK;
    }
}
