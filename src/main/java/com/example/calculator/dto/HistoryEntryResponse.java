package com.example.calculator.dto;

import com.example.calculator.model.CalculationHistory;

import java.time.format.DateTimeFormatter;

/** API response DTO for a single calculation history entry. Excludes id and username. */
public record HistoryEntryResponse(
        double operandA,
        double operandB,
        String operation,
        double result,
        String calculatedAt
) {
    public static HistoryEntryResponse from(CalculationHistory h) {
        return new HistoryEntryResponse(
                h.getOperandA(),
                h.getOperandB(),
                h.getOperation(),
                h.getResult(),
                DateTimeFormatter.ISO_INSTANT.format(h.getCalculatedAt())
        );
    }
}
