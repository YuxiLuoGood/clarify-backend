package com.expensetracker.expense_tracker.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.expensetracker.expense_tracker.dto.TransactionRequest;
import com.expensetracker.expense_tracker.model.Transaction;
import com.expensetracker.expense_tracker.model.User;
import com.expensetracker.expense_tracker.repository.TransactionRepository;
import com.expensetracker.expense_tracker.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository txRepo;
    private final UserRepository userRepo;
    private final BudgetAlertService budgetAlertService;

    // 查某个月的所有交易
    public List<Transaction> findAll(String email, String month) {
        if (month == null) {
            // 没有传月份就返回所有交易
            return txRepo.findAll();
        }
        return txRepo.findByUserAndMonth(email, month);
    }

    // 新增一条交易
    public Transaction create(String email, TransactionRequest req) {
        User user = getUser(email);

        Transaction tx = Transaction.builder()
            .user(user)
            .amount(req.getAmount())
            .category(req.getCategory())
            .description(req.getDescription())
            .type(req.getType())
            .date(req.getDate())
            .build();

        Transaction saved = txRepo.save(tx);

        // Check budget and send WebSocket alert if exceeded
        if ("EXPENSE".equals(req.getType())) {
            budgetAlertService.checkAndAlert(email, req.getCategory());
        }

        return saved;
    }

    // 修改一条交易
    public Transaction update(String email, UUID id, TransactionRequest req) {
        Transaction tx = txRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Transaction not found"));

        // 确保只能修改自己的交易
        if (!tx.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Not authorized");
        }

        tx.setAmount(req.getAmount());
        tx.setCategory(req.getCategory());
        tx.setDescription(req.getDescription());
        tx.setType(req.getType());
        tx.setDate(req.getDate());

        return txRepo.save(tx);
    }

    // 删除一条交易
    public void delete(String email, UUID id) {
        Transaction tx = txRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Transaction not found"));

        // 确保只能删除自己的交易
        if (!tx.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Not authorized");
        }

        txRepo.delete(tx);
    }

    private User getUser(String email) {
        return userRepo.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
}