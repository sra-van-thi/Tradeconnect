package com.infy.tradeconnect.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infy.tradeconnect.dto.TransactionDTO;
import com.infy.tradeconnect.entity.Stock;
import com.infy.tradeconnect.entity.StockAudit;
import com.infy.tradeconnect.entity.Transaction;
import com.infy.tradeconnect.exception.TradeConnectException;
import com.infy.tradeconnect.repository.StockAuditRepository;
import com.infy.tradeconnect.repository.StockRepository;
import com.infy.tradeconnect.repository.TransactionRepository;
import com.infy.tradeconnect.util.TradeConnectConstants;

@Service(value = "tradeConnectService")
@PropertySource("classpath:ValidationMessages.properties")
@Transactional
public class TradeConnectServiceImpl implements TradeConnectService {

    private static final Log LOGGER = LogFactory.getLog(TradeConnectServiceImpl.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockAuditRepository stockAuditRepository;

    @Autowired
    private Environment environment;

    // Helper method assumed to be present in stub for DTO translation
    private TransactionDTO convertToTransactionDTO(Transaction transaction) {
        TransactionDTO dto = new TransactionDTO();
        dto.setTransactionId(transaction.getTransactionId());
        dto.setUserId(transaction.getUserId());
        dto.setQuantity(transaction.getQuantity());
        if (transaction.getStock() != null) {
            dto.setStockId(transaction.getStock().getStockId());
            dto.setStockAvailable(transaction.getStock().getStockAvailable());
        }
        return dto;
    }

    // =========================================================================
    // 4.2. Implement Quantity Update Logic
    // =========================================================================
    @Override
    public List<TransactionDTO> updateBulkQuantity(String userId, Double minPrice, Integer stockId, Integer newQuantity)
     throws TradeConnectException {

        // 4.2.1. Validate input parameters
        Integer absoluteMaxQuantity = environment.getProperty("ABSOLUTE_MAX_QUANTITY",
         Integer.class, 10000);
        if (newQuantity > absoluteMaxQuantity) {
            throw new TradeConnectException(TradeConnectConstants.TRANSACTION_MAX_QUANTITY_EXCEEDS.toString());
        }

        // 4.2.2. Verify user existence
        List<Transaction> userTransactions = transactionRepository.findByUserId(userId);
        if (userTransactions == null || userTransactions.isEmpty()) {
            throw new TradeConnectException(TradeConnectConstants.TRANSACTION_NOT_FOUND_FOR_USERID.toString());
        }

        // 4.2.3. Identify eligible transactions
        List<Transaction> eligibleTransactions = userTransactions.stream()
                .filter(t -> t != null)
                .filter(t -> t.getStock() != null && t.getStock().getStockPrice() != null)
                .filter(t -> t.getStock().getStockPrice() > minPrice)
                .filter(t -> t.getStock().getStockId() != null && t.getStock().getStockId().equals(stockId))
                .collect(Collectors.toList());

        if (eligibleTransactions.isEmpty()) {
            throw new TradeConnectException(TradeConnectConstants.TRANSACTION_NOT_FOUND.toString());
        }

        // 4.2.4. Update stock inventory
        List<Integer> eligibleTransactionIds = eligibleTransactions.stream()
                .map(Transaction::getTransactionId)
                .collect(Collectors.toList());

        for (Transaction transaction : eligibleTransactions) {
            Integer oldQuantity = transaction.getQuantity();
            Integer quantityDelta = oldQuantity - newQuantity;
            stockRepository.updateStockAvailable(transaction.getStock().getStockId(), quantityDelta);
        }

        // 4.2.5. Bulk update transaction quantities
        int updatedCount = transactionRepository.updateBulkQuantity(userId, minPrice, stockId, newQuantity);

        // 4.2.6. Post-update verification and statistics
        List<Transaction> reFetchedTransactions = transactionRepository.findByUserId(userId);
        List<Transaction> updatedTransactions = reFetchedTransactions.stream()
                .filter(t -> eligibleTransactionIds.contains(t.getTransactionId()))
                .collect(Collectors.toList());

        long verifiedUpdates = updatedTransactions.stream()
                .filter(t -> t.getQuantity().equals(newQuantity))
                .count();

        if (verifiedUpdates != updatedCount) {
            LOGGER.warn("Update verification mismatch. Expected: " + updatedCount + ", Verified: " + verifiedUpdates);
        }
        LOGGER.info("Verified updates count: " + verifiedUpdates);

        // 4.2.7. Prepare and return response DTOs
        return updatedTransactions.stream()
                .map(this::convertToTransactionDTO)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // 4.3. Implement Delete Transactions by User ID
    // =========================================================================
    @Override
    public Integer deleteTransactionByUserId(String userId) throws TradeConnectException {

        // 4.3.1. Query transactions for the userId
        List<Transaction> userTransactions = transactionRepository.findByUserId(userId);
        if (userTransactions == null || userTransactions.isEmpty()) {
            throw new TradeConnectException(TradeConnectConstants.USER_NOT_FOUND.toString());
        }

        // 4.3.2. Restore stock availability before deletion
        for (Transaction transaction : userTransactions) {
            Integer availableAfter = null;
            Stock stock = transaction.getStock();

            if (stock != null && stock.getStockId() != null) {
                // Fetch the current available quantity from stock repository
                Stock currentStock = stockRepository.findById(stock.getStockId()).orElse(null);
                if (currentStock != null) {
                    availableAfter = currentStock.getStockAvailable();
                }
            }

            StockAudit audit = new StockAudit();
            audit.setStock(stock); // Will capture target reference or null validation boundary
            audit.setTransactionId(transaction.getTransactionId());
            audit.setQuantityChange(transaction.getQuantity()); // Representing stock release
            audit.setAvailableAfter(availableAfter);
            audit.setActionType("STOCK RELEASE");
            audit.setReferenceId(userId);
            audit.setActionTimestamp(LocalDateTime.now());

            stockAuditRepository.save(audit);
        }

        // 4.3.3. Delete all transactions for the user
        int deletedCount = transactionRepository.deleteByUserId(userId);

        // 4.3.5. Return the response
        return deletedCount;
    }

    // =========================================================================
    // 4.4. Implement Delete Transactions by Transaction ID
    // =========================================================================
    @Override
    public String deleteTransactionById(Integer transactionId) throws TradeConnectException {

        Transaction transaction = transactionRepository.findById(transactionId).orElse(null);

        // 4.4.1. Validate Stock Linkage
        if (transaction == null) {
            throw new TradeConnectException(TradeConnectConstants.TRANSACTION_NOT_FOUND.toString());
        }
        if (transaction.getStock() == null) {
            throw new TradeConnectException(TradeConnectConstants.TRANSACTION_NOT_FOUND.toString());
        }

        // 4.4.2. Validate associated stock information
        Stock stock = transaction.getStock();
        if (stock.getStockId() == null) {
            throw new TradeConnectException(TradeConnectConstants.TRANSACTION_STOCK_INVALID.toString());
        }

        // 4.4.3. Record Stock Audit/ Restore Inventory
        Integer availableAfter = null;
        Stock currentStock = stockRepository.findById(stock.getStockId()).orElse(null);
        if (currentStock != null) {
            availableAfter = currentStock.getStockAvailable();
        }

        StockAudit audit = new StockAudit();
        audit.setStock(stock);
        audit.setTransactionId(transaction.getTransactionId());
        audit.setQuantityChange(transaction.getQuantity()); // Representing released stock
        audit.setAvailableAfter(availableAfter);
        audit.setActionType("STOCK RELEASE");
        audit.setReferenceId(String.valueOf(transactionId));
        audit.setActionTimestamp(LocalDateTime.now());

        stockAuditRepository.save(audit);

        // Perform single instance removal
        transactionRepository.delete(transaction);

        return "Transaction deleted successfully";
    }
}