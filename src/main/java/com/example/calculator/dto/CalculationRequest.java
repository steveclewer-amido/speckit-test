package com.example.calculator.dto;

import com.example.calculator.service.Operation;
import jakarta.validation.constraints.NotNull;

/** Request body for POST /api/calculate. */
public class CalculationRequest {

    @NotNull(message = "Field 'a' must not be null.")
    private Double a;

    @NotNull(message = "Field 'b' must not be null.")
    private Double b;

    @NotNull(message = "Field 'operation' must not be null.")
    private Operation operation;

    public Double getA() { return a; }
    public void setA(Double a) { this.a = a; }

    public Double getB() { return b; }
    public void setB(Double b) { this.b = b; }

    public Operation getOperation() { return operation; }
    public void setOperation(Operation operation) { this.operation = operation; }
}
