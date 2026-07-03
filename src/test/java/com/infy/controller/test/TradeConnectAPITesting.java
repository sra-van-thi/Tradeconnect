package com.infy.controller.test;

import com.infy.tradeconnect.controller.TradeConnectAPI;
import com.infy.tradeconnect.service.TradeConnectService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest(TradeConnectAPI.class)
public class TradeConnectAPITesting {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradeConnectService tradeConnectService;

    // =========================================================================
    // 7.3. Valid Transaction Deletion Test
    // =========================================================================
    @Test
    public void testDeleteTransactionValidScenario() throws Exception {
        // 7.3.3. Prepare the required input data
        String userId = "USER9001";
        Integer expectedDeletedCount = 3;

        // 7.3.4. Mock the service layer behavior
        Mockito.when(tradeConnectService.deleteTransactionByUserId(userId))
                .thenReturn(expectedDeletedCount);

        // 7.3.5. Execute an HTTP DELETE request to the endpoint
        mockMvc.perform(MockMvcRequestBuilders.delete("/trades/transactions")
                        .param("userid", userId)
                        .accept(MediaType.APPLICATION_JSON))
                // 7.3.6. Validate the response behavior
                .andExpect(MockMvcResultMatchers.status().isOk())
                // 7.3.7. Enforce complete response validation (Exact Message Match)
                .andExpect(MockMvcResultMatchers.content().json("\"Deleted 3 transaction(s) for user: USER9001\""));
    }
}