package com.example.calculator.controller;

import com.example.calculator.config.SecurityConfig;
import com.example.calculator.dto.CalculationRequest;
import com.example.calculator.model.CalculationHistory;
import com.example.calculator.repository.CalculationHistoryRepository;
import com.example.calculator.service.CalculatorService;
import com.example.calculator.service.Operation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CalculatorController.class)
@Import({CalculatorService.class, SecurityConfig.class})
@TestPropertySource(properties = {"app.security.username=testuser", "app.security.password={noop}testpass"})
@WithMockUser
class CalculatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CalculationHistoryRepository historyRepo;

    // --- Helpers ---

    private String body(double a, double b, Operation op) throws Exception {
        CalculationRequest req = new CalculationRequest();
        req.setA(a);
        req.setB(b);
        req.setOperation(op);
        return objectMapper.writeValueAsString(req);
    }

    // --- US1: successful calculations (spec AC-1 through AC-4) ---

    @Test
    void add_returns200WithResult() throws Exception {
        mockMvc.perform(post("/api/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(2, 3, Operation.ADD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(5.0));
    }

    @Test
    void subtract_returns200WithResult() throws Exception {
        mockMvc.perform(post("/api/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(10, 4, Operation.SUBTRACT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(6.0));
    }

    @Test
    void multiply_returns200WithResult() throws Exception {
        mockMvc.perform(post("/api/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(6, 7, Operation.MULTIPLY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(42.0));
    }

    @Test
    void divide_returns200WithResult() throws Exception {
        mockMvc.perform(post("/api/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(8, 2, Operation.DIVIDE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(4.0));
    }

    // --- US2: validation errors (spec AC-5, AC-6) ---

    @Test
    void missingA_returns400WithError() throws Exception {
        mockMvc.perform(post("/api/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"b\":3,\"operation\":\"ADD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void missingB_returns400WithError() throws Exception {
        mockMvc.perform(post("/api/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":3,\"operation\":\"ADD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void invalidOperation_returns400WithError() throws Exception {
        mockMvc.perform(post("/api/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":2,\"b\":3,\"operation\":\"MODULO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void divideByZero_returns400WithDomainError() throws Exception {
        mockMvc.perform(post("/api/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(5, 0, Operation.DIVIDE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Division by zero is not allowed."));
    }

    @Test
    void nonNumericValue_returns400WithError() throws Exception {
        mockMvc.perform(post("/api/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":\"hello\",\"b\":3,\"operation\":\"ADD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid number value."));
    }

    // --- History: US1 GET /api/history ---

    @Test
    void getHistory_authenticated_returns200WithJsonArray() throws Exception {
        given(historyRepo.findTop20ByUsernameOrderByCalculatedAtDesc(anyString()))
                .willReturn(List.of());

        mockMvc.perform(get("/api/history"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[]"));
    }

    @Test
    @WithAnonymousUser
    void getHistory_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/history"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required."));
    }

    @Test
    void calculate_success_savesHistoryEntry() throws Exception {
        mockMvc.perform(post("/api/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(2, 3, Operation.ADD)))
                .andExpect(status().isOk());

        verify(historyRepo).save(any(CalculationHistory.class));
    }

    @Test
    void calculate_divisionByZero_doesNotSaveHistoryEntry() throws Exception {
        mockMvc.perform(post("/api/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(5, 0, Operation.DIVIDE)))
                .andExpect(status().isBadRequest());

        verify(historyRepo, never()).save(any());
    }

    // --- History: US3 DELETE /api/history ---

    @Test
    void deleteHistory_authenticated_returns204() throws Exception {
        mockMvc.perform(delete("/api/history"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithAnonymousUser
    void deleteHistory_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/history"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required."));
    }
}
