package com.clarify.stats;

import com.clarify.transaction.Transaction;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Read-only aggregate queries owned by the stats feature. Portable JPQL
 * (no TO_CHAR / DATE_TRUNC), so it isn't tied to one database's functions.
 */
public interface StatsQueries extends Repository<Transaction, UUID> {

    @Query("""
            select new com.clarify.stats.TypeTotal(t.type, sum(t.amount))
            from Transaction t
            where t.userId = :userId and t.date >= :from and t.date < :to
            group by t.type
            """)
    List<TypeTotal> totalsByType(@Param("userId") UUID userId,
                                 @Param("from") LocalDate from,
                                 @Param("to") LocalDate to);

    @Query("""
            select new com.clarify.stats.CategoryTotal(t.category, sum(t.amount))
            from Transaction t
            where t.userId = :userId and t.type = com.clarify.transaction.TransactionType.EXPENSE
              and t.date >= :from and t.date < :to
            group by t.category
            order by sum(t.amount) desc
            """)
    List<CategoryTotal> expensesByCategory(@Param("userId") UUID userId,
                                           @Param("from") LocalDate from,
                                           @Param("to") LocalDate to);

    @Query("""
            select new com.clarify.stats.MonthlyTypeTotal(year(t.date), month(t.date), t.type, sum(t.amount))
            from Transaction t
            where t.userId = :userId and t.date >= :from and t.date < :to
            group by year(t.date), month(t.date), t.type
            """)
    List<MonthlyTypeTotal> monthlyTotals(@Param("userId") UUID userId,
                                         @Param("from") LocalDate from,
                                         @Param("to") LocalDate to);
}
