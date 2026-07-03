package com.infy.tradeconnect.service;

import java.util.List;

import com.infy.tradeconnect.dto.TransactionDTO;
import com.infy.tradeconnect.exception.TradeConnectException;

public interface TradeConnectService {

    List<TransactionDTO> updateBulkQuantity(String userId, Double minPrice, Integer stockId, Integer newQuantity) throws TradeConnectException;

    Integer deleteTransactionByUserId(String userId) throws TradeConnectException;

    String deleteTransactionById(Integer transactionId) throws TradeConnectException;
}
