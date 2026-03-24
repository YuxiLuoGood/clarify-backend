package com.expensetracker.expense_tracker.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.expensetracker.expense_tracker.dto.TransactionRequest;
import com.expensetracker.expense_tracker.model.Transaction;
import com.expensetracker.expense_tracker.model.User;
import com.expensetracker.expense_tracker.repository.TransactionRepository;
import com.expensetracker.expense_tracker.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository txRepo;
    @Mock private UserRepository userRepo;
    @Mock private BudgetAlertService budgetAlertService;

    @InjectMocks
    private TransactionService transactionService;

    private User user;
    private TransactionRequest req;

    @BeforeEach
    void setUp() {
        user = User.builder()
            .email("test@example.com")
            .name("Test User")
            .password("hashed")
            .build();

        req = new TransactionRequest();
        req.setAmount(new BigDecimal("50.00"));
        req.setCategory("FOOD");
        req.setDescription("Lunch");
        req.setType("EXPENSE");
        req.setDate(LocalDate.now());
    }

    @Test
    void create_success_savesTransaction() {
        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(txRepo.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        Transaction result = transactionService.create("test@example.com", req);

        assertNotNull(result);
        assertEquals(new BigDecimal("50.00"), result.getAmount());
        assertEquals("FOOD", result.getCategory());
        assertEquals("EXPENSE", result.getType());
        verify(txRepo).save(any(Transaction.class));
        // Budget alert should be checked for EXPENSE
        verify(budgetAlertService).checkAndAlert("test@example.com", "FOOD");
    }

    @Test
    void create_income_doesNotTriggerBudgetAlert() {
        req.setType("INCOME");
        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(txRepo.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        transactionService.create("test@example.com", req);

        // Budget alert should NOT be checked for INCOME
        verify(budgetAlertService, never()).checkAndAlert(any(), any());
    }

    @Test
    void delete_unauthorized_throwsException() {
        User otherUser = User.builder()
            .email("other@example.com")
            .name("Other User")
            .password("hashed")
            .build();

        Transaction tx = Transaction.builder()
            .id(UUID.randomUUID())
            .user(otherUser)
            .amount(new BigDecimal("50.00"))
            .build();

        when(txRepo.findById(tx.getId())).thenReturn(Optional.of(tx));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> transactionService.delete("test@example.com", tx.getId()));

        assertEquals("Not authorized", ex.getMessage());
        verify(txRepo, never()).delete(any());
    }

    @Test
    void delete_authorized_deletesTransaction() {
        Transaction tx = Transaction.builder()
            .id(UUID.randomUUID())
            .user(user)
            .amount(new BigDecimal("50.00"))
            .build();

        when(txRepo.findById(tx.getId())).thenReturn(Optional.of(tx));

        transactionService.delete("test@example.com", tx.getId());

        verify(txRepo).delete(tx);
    }
}