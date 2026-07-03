package com.infy.tradeconnect.controller;

import com.infy.tradeconnect.dto.TransactionDTO;
import com.infy.tradeconnect.exception.TradeConnectException;
import com.infy.tradeconnect.service.TradeConnectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.Contact;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/trades")
@Validated
@OpenAPIDefinition(
        info = @Info(
                title = "Trade Connect API",
                version = "1.0",
                description = "API for managing stock transactions in Trade Connect application",
                contact = @Contact(
                        name = "Trade Connect Support",
                        email = "support@tradeconnect.com"
                )
        )
)
@Tag(name = "Transaction Management", description = "APIs for managing stock transactions")
public class TradeConnectAPI {

    @Autowired
    private TradeConnectService tradeConnectService;

    // =========================================================================
    // 5.2. Transaction Bulk Update Endpoint
    // =========================================================================
    @PutMapping(
            value = "/transactions/bulk-update/quantity",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<TransactionDTO>> updateBulkQuantity(
            @RequestParam("userid")
            @NotBlank(message = "{user.id.not.valid}") String userid,

            @RequestParam("minPrice")
            @NotNull(message = "{stock.price.must}")
            @DecimalMin(value = "0.01", message = "{stock.price.decimal.min.invalid}") Double minPrice,

            @RequestParam("stockld")
            @NotNull(message = "{stock.id.not.valid}")
            @Min(value = 1, message = "{stock.id.must}") Integer stockid,

            @RequestParam("newQuantity")
            @NotNull(message = "{transaction.newQuantity.must}")
            @Min(value = 1, message = "{transaction.quantity.not.valid}")
            @Max(value = 10000, message = "{transaction.quantity.limit.exceeds}") Integer newQuantity
    ) throws TradeConnectException {

        List<TransactionDTO> updatedTransactions = tradeConnectService.updateBulkQuantity(userid, minPrice, stockid, newQuantity);
        return new ResponseEntity<>(updatedTransactions, HttpStatus.OK);
    }

    // =========================================================================
    // 5.3. Transaction Deletion by userid Endpoint
    // =========================================================================
    @DeleteMapping(
            value = "/transactions",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Delete transactions by User ID",
            description = "Deletes all transactions associated with the specified user ID. This operation also restores the stock quantities for all deleted transactions."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions successfully deleted. Returns the count of deleted transactions."),
            @ApiResponse(responseCode = "400", description = "No transactions found for the specified user ID"),
            @ApiResponse(responseCode = "500", description = "Internal server error occurred while processing the request")
    })
    public ResponseEntity<String> deleteTransaction(
            @RequestParam("userid")
            @NotBlank(message = "{user.id.not.valid}") String userid
    ) throws TradeConnectException {

        Integer deletedCount = tradeConnectService.deleteTransactionByUserId(userid);
        String responseMessage = "Deleted " + deletedCount + " transaction(s) for user: " + userid;

        // Output must wrap properly as a JSON formatted string layout or literal string response
        return new ResponseEntity<>("\"" + responseMessage + "\"", HttpStatus.OK);
    }

    // =========================================================================
    // 5.4. Transaction Deletion by Transaction ID Endpoint (V2)
    // =========================================================================
    @DeleteMapping(
            value = "/transactions/{transactionld}",
            params = "version=2",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> deleteTransactionV2(
            @PathVariable("transactionld")
            @Min(value = 1, message = "Transaction ID must be a positive numeric value") Integer transactionld
    ) throws TradeConnectException {

        String statusMessage = tradeConnectService.deleteTransactionById(transactionld);
        return new ResponseEntity<>("\"" + statusMessage + "\"", HttpStatus.OK);
    }
}