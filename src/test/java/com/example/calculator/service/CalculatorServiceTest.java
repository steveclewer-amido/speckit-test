package com.example.calculator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CalculatorServiceTest {

    private CalculatorService service;

    @BeforeEach
    void setUp() {
        service = new CalculatorService();
    }

    // --- US1: happy-path acceptance scenarios (spec AC-1 through AC-4) ---

    @Test
    void add_returnsSumOfTwoIntegers() {
        assertThat(service.calculate(2, 3, Operation.ADD)).isEqualTo(5.0);
    }

    @Test
    void subtract_returnsDifference() {
        assertThat(service.calculate(10, 4, Operation.SUBTRACT)).isEqualTo(6.0);
    }

    @Test
    void multiply_returnsProduct() {
        assertThat(service.calculate(6, 7, Operation.MULTIPLY)).isEqualTo(42.0);
    }

    @Test
    void divide_returnsQuotient() {
        assertThat(service.calculate(8, 2, Operation.DIVIDE)).isEqualTo(4.0);
    }

    // --- Spec-declared edge cases ---

    @Test
    void add_withNegativeOperand_returnsCorrectResult() {
        assertThat(service.calculate(-5, 2, Operation.ADD)).isEqualTo(-3.0);
    }

    @Test
    void multiply_withDecimalOperands_returnsCorrectResult() {
        assertThat(service.calculate(1.5, 2, Operation.MULTIPLY)).isEqualTo(3.0);
    }

    // --- US2: divide-by-zero (spec AC-6) ---

    @Test
    void divide_byZero_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.calculate(5, 0, Operation.DIVIDE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Division by zero is not allowed.");
    }
}
