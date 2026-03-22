package com.expensetracker.expense_tracker.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String category;  // FOOD, TRANSPORT, SHOPPING, BILLS, OTHER

    private String description;

    @Column(nullable = false)
    private String type;      // INCOME or EXPENSE

    @Column(nullable = false)
    private LocalDate date;

    private LocalDateTime createdAt = LocalDateTime.now();
}