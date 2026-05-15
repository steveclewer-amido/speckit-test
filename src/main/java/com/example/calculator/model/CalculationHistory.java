package com.example.calculator.model;

import jakarta.persistence.*;
import java.time.Instant;

/** JPA entity representing a single saved calculation for an authenticated user. */
@Entity
@Table(
    name = "CALCULATION_HISTORY",
    indexes = @Index(name = "idx_history_username_at", columnList = "USERNAME, CALCULATED_AT DESC")
)
public class CalculationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "USERNAME", nullable = false)
    private String username;

    @Column(name = "OPERAND_A", nullable = false)
    private Double operandA;

    @Column(name = "OPERAND_B", nullable = true)
    private Double operandB;

    @Column(name = "OPERATION", nullable = false, length = 20)
    private String operation;

    @Column(name = "RESULT", nullable = false)
    private Double result;

    @Column(name = "CALCULATED_AT", nullable = false)
    private Instant calculatedAt;

    public Long getId() { return id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Double getOperandA() { return operandA; }
    public void setOperandA(Double operandA) { this.operandA = operandA; }

    public Double getOperandB() { return operandB; }
    public void setOperandB(Double operandB) { this.operandB = operandB; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public Double getResult() { return result; }
    public void setResult(Double result) { this.result = result; }

    public Instant getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(Instant calculatedAt) { this.calculatedAt = calculatedAt; }
}
