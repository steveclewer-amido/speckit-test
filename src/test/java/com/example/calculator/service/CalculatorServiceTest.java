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
        assertThat(service.calculate(2, 3.0, Operation.ADD)).isEqualTo(5.0);
    }

    @Test
    void subtract_returnsDifference() {
        assertThat(service.calculate(10, 4.0, Operation.SUBTRACT)).isEqualTo(6.0);
    }

    @Test
    void multiply_returnsProduct() {
        assertThat(service.calculate(6, 7.0, Operation.MULTIPLY)).isEqualTo(42.0);
    }

    @Test
    void divide_returnsQuotient() {
        assertThat(service.calculate(8, 2.0, Operation.DIVIDE)).isEqualTo(4.0);
    }

    // --- Spec-declared edge cases ---

    @Test
    void add_withNegativeOperand_returnsCorrectResult() {
        assertThat(service.calculate(-5, 2.0, Operation.ADD)).isEqualTo(-3.0);
    }

    @Test
    void multiply_withDecimalOperands_returnsCorrectResult() {
        assertThat(service.calculate(1.5, 2.0, Operation.MULTIPLY)).isEqualTo(3.0);
    }

    // --- US2: divide-by-zero (spec AC-6) ---

    @Test
    void divide_byZero_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.calculate(5, 0.0, Operation.DIVIDE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Division by zero is not allowed.");
    }

    // --- T007: Scientific operations — 6 valid-result tests ---

    @Test
    void sqrt_ofNine_returnsThree() {
        assertThat(service.calculate(9, null, Operation.SQRT)).isEqualTo(3.0);
    }

    @Test
    void power_twoToTheEighth_returns256() {
        assertThat(service.calculate(2, 8.0, Operation.POWER)).isEqualTo(256.0);
    }

    @Test
    void ln_ofOne_returnsZero() {
        assertThat(service.calculate(1, null, Operation.LN)).isEqualTo(0.0);
    }

    @Test
    void sin_ofNinetyDegrees_returnsOne() {
        assertThat(service.calculate(90, null, Operation.SIN)).isCloseTo(1.0, within(1e-10));
    }

    @Test
    void cos_ofZeroDegrees_returnsOne() {
        assertThat(service.calculate(0, null, Operation.COS)).isEqualTo(1.0);
    }

    @Test
    void tan_ofFortyFiveDegrees_returnsOne() {
        assertThat(service.calculate(45, null, Operation.TAN)).isCloseTo(1.0, within(1e-10));
    }

    // --- T007: Scientific operations — 5 domain-error tests ---

    @Test
    void sqrt_ofNegative_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.calculate(-1, null, Operation.SQRT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Square root of a negative number is undefined.");
    }

    @Test
    void ln_ofZero_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.calculate(0, null, Operation.LN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Natural logarithm of zero or a negative number is undefined.");
    }

    @Test
    void ln_ofNegative_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.calculate(-1, null, Operation.LN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Natural logarithm of zero or a negative number is undefined.");
    }

    @Test
    void tan_ofNinetyDegrees_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.calculate(90, null, Operation.TAN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tan(90\u00b0) is undefined.");
    }

    @Test
    void power_withoutB_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.calculate(2, null, Operation.POWER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Second operand is required for POWER.");
    }
}
