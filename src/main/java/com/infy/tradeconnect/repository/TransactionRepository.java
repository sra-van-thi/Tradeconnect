package com.infy.tradeconnect.repository;

import com.infy.tradeconnect.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    // Helper method to look up transactions by user id as required by the service layer
    List<Transaction> findByUserId(String userId);

    @Modifying
    @Query("UPDATE Transaction t SET t.quantity = :newQuantity WHERE t.userId = :userId AND t.stock.stockPrice > :minPrice AND t.stock.stockId = :stockId")
    int updateBulkQuantity(@Param("userId") String userId,
                           @Param("minPrice") Double minPrice,
                           @Param("stockId") Integer stockId,
                           @Param("newQuantity") Integer newQuantity);

    @Modifying
    @Query("DELETE FROM Transaction t WHERE t.userId = :userId")
    int deleteByUserId(@Param("userId") String userId);
}