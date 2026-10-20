package com.clarify.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Every query is scoped by user id: ownership is enforced by the query itself,
 * not by a check that could be forgotten afterwards.
 * Months are [from, to) date ranges, which can use idx_tx_user_date.
 */
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            select t from Transaction t
            where t.userId = :userId
            order by t.date desc, t.createdAt desc
            """)
    List<Transaction> findAllForUser(@Param("userId") UUID userId);

    @Query("""
            select t from Transaction t
            where t.userId = :userId and t.date >= :from and t.date < :to
            order by t.date desc, t.createdAt desc
            """)
    List<Transaction> findForUserBetween(@Param("userId") UUID userId,
                                         @Param("from") LocalDate from,
                                         @Param("to") LocalDate to);

    /** Null when nothing matches. */
    @Query("""
            select sum(t.amount) from Transaction t
            where t.userId = :userId and t.type = :type and t.category = :category
              and t.date >= :from and t.date < :to
            """)
    BigDecimal sumAmount(@Param("userId") UUID userId,
                         @Param("type") TransactionType type,
                         @Param("category") Category category,
                         @Param("from") LocalDate from,
                         @Param("to") LocalDate to);
}
