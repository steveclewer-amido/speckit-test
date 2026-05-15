package com.example.calculator.service;

import org.springframework.stereotype.Service;

/** Stateless arithmetic and scientific service. All ten operations are supported. */
@Service
public class CalculatorService {

    /**
     * Performs the requested arithmetic or scientific operation.
     *
     * @param a         first operand (always required)
     * @param b         second operand (required for binary operations; null for unary)
     * @param operation the operation to apply
     * @return the computed result
     * @throws IllegalArgumentException on domain violations (division by zero, sqrt of negative, etc.)
     */
    public double calculate(double a, Double b, Operation operation) {
        return switch (operation) {
            case ADD -> {
                requireB(b, "ADD");
                yield a + b;
            }
            case SUBTRACT -> {
                requireB(b, "SUBTRACT");
                yield a - b;
            }
            case MULTIPLY -> {
                requireB(b, "MULTIPLY");
                yield a * b;
            }
            case DIVIDE -> {
                requireB(b, "DIVIDE");
                if (b == 0) throw new IllegalArgumentException("Division by zero is not allowed.");
                yield a / b;
            }
            case SQRT -> {
                if (a < 0) throw new IllegalArgumentException("Square root of a negative number is undefined.");
                yield Math.sqrt(a);
            }
            case POWER -> {
                requireB(b, "POWER");
                yield Math.pow(a, b);
            }
            case LN -> {
                if (a <= 0) throw new IllegalArgumentException("Natural logarithm of zero or a negative number is undefined.");
                yield Math.log(a);
            }
            case SIN -> Math.sin(Math.toRadians(a));
            case COS -> Math.cos(Math.toRadians(a));
            case TAN -> {
                if (Math.abs(a % 180) == 90) throw new IllegalArgumentException("tan(90\u00b0) is undefined.");
                yield Math.tan(Math.toRadians(a));
            }
        };
    }

    private void requireB(Double b, String opName) {
        if (b == null) {
            throw new IllegalArgumentException("Second operand is required for " + opName + ".");
        }
    }
}
