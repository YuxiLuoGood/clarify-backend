package com.clarify.transaction;

import com.clarify.common.error.NotFoundException;
import com.clarify.security.CurrentUser;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TransactionService {

    static final String NOT_FOUND = "Transaction not found";

    private final TransactionRepository transactions;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public TransactionService(TransactionRepository transactions, ApplicationEventPublisher events, Clock clock) {
        this.transactions = transactions;
        this.events = events;
        this.clock = clock;
    }

    /** The caller's transactions, newest first; all of them when {@code month} is null. */
    @Transactional(readOnly = true)
    public List<TransactionResponse> list(UUID userId, @Nullable YearMonth month) {
        List<Transaction> found = month == null
                ? transactions.findAllForUser(userId)
                : transactions.findForUserBetween(userId, month.atDay(1), month.plusMonths(1).atDay(1));
        return found.stream().map(TransactionResponse::from).toList();
    }

    public TransactionResponse create(CurrentUser user, TransactionRequest request) {
        Transaction tx = transactions.save(Transaction.create(user.id(), request.amount(), request.category(),
                request.description(), request.type(), request.date(), LocalDateTime.now(clock)));
        events.publishEvent(new TransactionCreatedEvent(tx.getId(), user.id(), user.email(),
                tx.getType(), tx.getCategory(), tx.getAmount(), tx.getDate()));
        return TransactionResponse.from(tx);
    }

    public TransactionResponse update(UUID userId, UUID id, TransactionRequest request) {
        Transaction tx = findOwned(userId, id);
        tx.update(request.amount(), request.category(), request.description(), request.type(), request.date());
        return TransactionResponse.from(tx);   // flushed on commit by dirty checking
    }

    public void delete(UUID userId, UUID id) {
        transactions.delete(findOwned(userId, id));
    }

    /** Someone else's transaction is reported exactly like a missing one. */
    private Transaction findOwned(UUID userId, UUID id) {
        return transactions.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));
    }
}
