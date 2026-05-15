package com.example.calculator.controller;

import com.example.calculator.dto.CalculationRequest;
import com.example.calculator.dto.CalculationResponse;
import com.example.calculator.dto.HistoryEntryResponse;
import com.example.calculator.model.CalculationHistory;
import com.example.calculator.repository.CalculationHistoryRepository;
import com.example.calculator.service.CalculatorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
import java.util.List;

/** REST controller exposing the calculator API. */
@RestController
@RequestMapping("/api")
public class CalculatorController {

    private final CalculatorService calculatorService;
    private final CalculationHistoryRepository historyRepository;

    public CalculatorController(CalculatorService calculatorService,
                                CalculationHistoryRepository historyRepository) {
        this.calculatorService = calculatorService;
        this.historyRepository = historyRepository;
    }

    /**
     * Performs an arithmetic calculation and saves the result to history.
     *
     * @param request   validated request body
     * @param principal authenticated user identity
     * @return 200 with {@code {"result": ...}} on success, or 400 with {@code {"error": "..."}} on failure
     */
    @PostMapping("/calculate")
    public ResponseEntity<CalculationResponse> calculate(
            @Valid @RequestBody CalculationRequest request, Principal principal) {
        double result = calculatorService.calculate(
                request.getA(), request.getB(), request.getOperation());

        CalculationHistory entry = new CalculationHistory();
        entry.setUsername(principal.getName());
        entry.setOperandA(request.getA());
        entry.setOperandB(request.getB());
        entry.setOperation(request.getOperation().name());
        entry.setResult(result);
        entry.setCalculatedAt(Instant.now());
        historyRepository.save(entry);

        return ResponseEntity.ok(CalculationResponse.success(result));
    }

    /**
     * Returns the authenticated user's 20 most recent calculations, newest first.
     *
     * @param principal authenticated user identity
     * @return 200 with JSON array of history entries (may be empty)
     */
    @GetMapping("/history")
    public ResponseEntity<List<HistoryEntryResponse>> getHistory(Principal principal) {
        List<HistoryEntryResponse> history = historyRepository
                .findTop20ByUsernameOrderByCalculatedAtDesc(principal.getName())
                .stream()
                .map(HistoryEntryResponse::from)
                .toList();
        return ResponseEntity.ok(history);
    }

    /**
     * Deletes all calculation history for the authenticated user. Idempotent.
     *
     * @param principal authenticated user identity
     * @return 204 No Content
     */
    @DeleteMapping("/history")
    public ResponseEntity<Void> clearHistory(Principal principal) {
        historyRepository.deleteAllByUsername(principal.getName());
        return ResponseEntity.noContent().build();
    }

    /** Handles Bean Validation failures (missing or null fields). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CalculationResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Validation failed.");
        return ResponseEntity.badRequest().body(CalculationResponse.error(message));
    }

    /** Handles domain errors such as division by zero. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CalculationResponse> handleDomainError(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(CalculationResponse.error(ex.getMessage()));
    }

    /** Handles malformed JSON or type-mismatch (e.g. non-numeric value for a numeric field). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CalculationResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(CalculationResponse.error("Invalid number value."));
    }
}
