package com.infy.tradeconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.infy.tradeconnect.entity.StockAudit;

public interface StockAuditRepository extends JpaRepository<StockAudit, Integer> {
}
