package com.example.calculator.dto;

/** Response body for POST /api/calculate. Exactly one of {@code result} or {@code error} is non-null. */
public class CalculationResponse {

    private Double result;
    private String error;

    private CalculationResponse() {}

    /** Creates a successful response carrying the computed value. */
    public static CalculationResponse success(double result) {
        CalculationResponse r = new CalculationResponse();
        r.result = result;
        return r;
    }

    /** Creates an error response carrying a human-readable message. */
    public static CalculationResponse error(String message) {
        CalculationResponse r = new CalculationResponse();
        r.error = message;
        return r;
    }

    public Double getResult() { return result; }
    public String getError() { return error; }
}
