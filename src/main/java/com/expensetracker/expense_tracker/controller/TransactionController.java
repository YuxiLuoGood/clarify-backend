package com.expensetracker.expense_tracker.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.expensetracker.expense_tracker.dto.TransactionRequest;
import com.expensetracker.expense_tracker.model.Transaction;
import com.expensetracker.expense_tracker.service.TransactionService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "CRUD operations for income and expense records")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService txService;

    // GET /api/transactions?month=2024-03
    @GetMapping
    public List<Transaction> getAll(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) String month) {
        return txService.findAll(user.getUsername(), month);
    }

    // POST /api/transactions
    @PostMapping
    public ResponseEntity<Transaction> create(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody TransactionRequest req) {
        return ResponseEntity.ok(txService.create(user.getUsername(), req));
    }

    // PUT /api/transactions/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Transaction> update(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID id,
            @Valid @RequestBody TransactionRequest req) {
        return ResponseEntity.ok(txService.update(user.getUsername(), id, req));
    }

    // DELETE /api/transactions/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID id) {
        txService.delete(user.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}