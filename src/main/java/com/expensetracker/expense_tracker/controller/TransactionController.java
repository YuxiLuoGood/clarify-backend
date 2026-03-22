package com.expensetracker.expense_tracker.controller;

import com.expensetracker.expense_tracker.dto.TransactionRequest;
import com.expensetracker.expense_tracker.model.Transaction;
import com.expensetracker.expense_tracker.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
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