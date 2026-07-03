package com.infy.tradeconnect.repository;

import com.infy.tradeconnect.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockRepository extends JpaRepository<Stock, Integer> {

    @Modifying
    @Query("UPDATE Stock s SET s.stockAvailable = s.stockAvailable + :quantityChange WHERE s.stockId = :stockId")
    int updateStockAvailable(@Param("stockId") Integer stockId, @Param("quantityChange") Integer quantityChange);
}