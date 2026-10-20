package com.clarify.alert;

import com.clarify.transaction.Category;
import com.clarify.transaction.TransactionCreatedEvent;
import com.clarify.transaction.TransactionRepository;
import com.clarify.transaction.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

/**
 * Pushes a budget alert when an expense takes its category over the monthly
 * limit.
 * <ul>
 *   <li>AFTER_COMMIT: only for transactions that were actually saved</li>
 *   <li>@Async: the HTTP response doesn't wait for the check or the push</li>
 *   <li>never throws: a failed alert must not affect the saved transaction</li>
 * </ul>
 */
@Service
public class BudgetAlertService {

    private static final Logger log = LoggerFactory.getLogger(BudgetAlertService.class);

    private final TransactionRepository transactions;
    private final BudgetProperties budget;
    private final AlertSessionRegistry registry;
    private final Clock clock;

    public BudgetAlertService(TransactionRepository transactions, BudgetProperties budget,
                              AlertSessionRegistry registry, Clock clock) {
        this.transactions = transactions;
        this.budget = budget;
        this.registry = registry;
        this.clock = clock;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onTransactionCreated(TransactionCreatedEvent event) {
        if (event.type() != TransactionType.EXPENSE) {
            return;
        }
        try {
            checkBudget(event.userId(), event.userEmail(), event.category());
        } catch (RuntimeException e) {
            log.warn("Budget check failed for transaction {}", event.transactionId(), e);
        }
    }

    /** Checks the current calendar month, as the alert text says "this month". */
    void checkBudget(UUID userId, String email, Category category) {
        YearMonth month = YearMonth.now(clock);
        BigDecimal spent = Objects.requireNonNullElse(
                transactions.sumAmount(userId, TransactionType.EXPENSE, category,
                        month.atDay(1), month.plusMonths(1).atDay(1)),
                BigDecimal.ZERO);
        BigDecimal limit = budget.limitFor(category);
        if (spent.compareTo(limit) > 0) {
            registry.send(email, BudgetAlert.exceeded(category, spent, limit));
        }
    }
}
