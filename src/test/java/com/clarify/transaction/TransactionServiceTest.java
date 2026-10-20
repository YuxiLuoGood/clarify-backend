package com.clarify.transaction;

import com.clarify.common.error.NotFoundException;
import com.clarify.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionServiceTest {

    private static final CurrentUser USER = new CurrentUser(UUID.randomUUID(), "test@example.com");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-15T10:00:00Z"), ZoneOffset.UTC);

    private TransactionRepository repository;
    private org.springframework.context.ApplicationEventPublisher events;
    private TransactionService service;

    @BeforeEach
    void setUp() {
        repository = mock(TransactionRepository.class);
        events = mock(org.springframework.context.ApplicationEventPublisher.class);
        service = new TransactionService(repository, events, CLOCK);
    }

    private static TransactionRequest lunch() {
        return new TransactionRequest(new BigDecimal("50.00"), Category.FOOD, "Lunch",
                TransactionType.EXPENSE, LocalDate.of(2026, 3, 15));
    }

    private static Transaction existing(UUID owner) {
        return Transaction.create(owner, BigDecimal.TEN, Category.OTHER, null, TransactionType.EXPENSE,
                LocalDate.of(2026, 3, 1), LocalDateTime.now(CLOCK));
    }

    @Test
    void createSavesAndPublishesAnEvent() {
        when(repository.save(any(Transaction.class))).then(returnsFirstArg());

        TransactionResponse response = service.create(USER, lunch());

        assertThat(response.amount()).isEqualByComparingTo("50.00");
        assertThat(response.category()).isEqualTo(Category.FOOD);
        assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 3, 15, 10, 0));

        ArgumentCaptor<TransactionCreatedEvent> event = ArgumentCaptor.forClass(TransactionCreatedEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().userEmail()).isEqualTo(USER.email());
        assertThat(event.getValue().type()).isEqualTo(TransactionType.EXPENSE);
        assertThat(event.getValue().category()).isEqualTo(Category.FOOD);
    }

    @Test
    void listForAMonthQueriesThatDateRange() {
        when(repository.findForUserBetween(USER.id(), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1)))
                .thenReturn(List.of(existing(USER.id())));

        assertThat(service.list(USER.id(), YearMonth.of(2026, 3))).hasSize(1);
    }

    @Test
    void updateOfAnotherUsersTransactionIsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUserId(id, USER.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(USER.id(), id, lunch()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Transaction not found");
    }

    @Test
    void updateChangesTheEntity() {
        UUID id = UUID.randomUUID();
        Transaction tx = existing(USER.id());
        when(repository.findByIdAndUserId(id, USER.id())).thenReturn(Optional.of(tx));

        service.update(USER.id(), id, lunch());

        assertThat(tx.getCategory()).isEqualTo(Category.FOOD);
        assertThat(tx.getDescription()).isEqualTo("Lunch");
    }

    @Test
    void deleteRemovesOwnedTransaction() {
        UUID id = UUID.randomUUID();
        Transaction tx = existing(USER.id());
        when(repository.findByIdAndUserId(id, USER.id())).thenReturn(Optional.of(tx));

        service.delete(USER.id(), id);

        verify(repository).delete(tx);
    }

    @Test
    void deleteOfAnotherUsersTransactionDeletesNothing() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUserId(id, USER.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(USER.id(), id)).isInstanceOf(NotFoundException.class);
        verify(repository, never()).delete(any());
    }
}
