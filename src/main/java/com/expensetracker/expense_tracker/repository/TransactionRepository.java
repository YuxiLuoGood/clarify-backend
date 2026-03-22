package com.expensetracker.expense_tracker.repository;

import com.expensetracker.expense_tracker.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    // 查某个用户某个月的所有交易
    @Query("""
        SELECT t FROM Transaction t
        WHERE t.user.email = :email
          AND FUNCTION('TO_CHAR', t.date, 'YYYY-MM') = :month
        ORDER BY t.date DESC
    """)
    List<Transaction> findByUserAndMonth(@Param("email") String email,
                                         @Param("month") String month);

    // 按类别统计支出（比如：FOOD用了多少钱）
    @Query("""
        SELECT t.category, SUM(t.amount)
        FROM Transaction t
        WHERE t.user.email = :email
          AND t.type = 'EXPENSE'
          AND FUNCTION('TO_CHAR', t.date, 'YYYY-MM') = :month
        GROUP BY t.category
    """)
    List<Object[]> sumByCategory(@Param("email") String email,
                                  @Param("month") String month);

    // 统计某月总收入或总支出
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.user.email = :email
          AND t.type = :type
          AND FUNCTION('TO_CHAR', t.date, 'YYYY-MM') = :month
    """)
    BigDecimal sumByType(@Param("email") String email,
                         @Param("type") String type,
                         @Param("month") String month);

    // 最近N个月的收支趋势
    @Query(value = """
        SELECT TO_CHAR(date, 'YYYY-MM') AS month,
               SUM(CASE WHEN type='EXPENSE' THEN amount ELSE 0 END) AS total_expense,
               SUM(CASE WHEN type='INCOME'  THEN amount ELSE 0 END) AS total_income
        FROM transactions
        WHERE user_id = (SELECT id FROM users WHERE email = :email)
          AND date >= DATE_TRUNC('month', NOW()) - INTERVAL '1 month' * :months
        GROUP BY TO_CHAR(date, 'YYYY-MM')
        ORDER BY month ASC
    """, nativeQuery = true)
    List<Object[]> monthlyTrend(@Param("email") String email,
                                 @Param("months") int months);
}