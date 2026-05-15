package com.example.calculator.service;

import org.springframework.stereotype.Service;

/** Stateless arithmetic service. All four operations are supported. */
@Service
public class CalculatorService {

    /**
     * Performs the requested arithmetic operation on two operands.
     *
     * @param a         first operand
     * @param b         second operand
     * @param operation the operation to apply
     * @return the computed result
     * @throws IllegalArgumentException if {@code operation} is {@link Operation#DIVIDE} and {@code b} is zero
     */
    public double calculate(double a, double b, Operation operation) {
        return switch (operation) {
            case ADD      -> a + b;
            case SUBTRACT -> a - b;
            case MULTIPLY -> a * b;
            case DIVIDE   -> {
                if (b == 0) {
                    throw new IllegalArgumentException("Division by zero is not allowed.");
                }
                yield a / b;
            }
        };
    }
}
