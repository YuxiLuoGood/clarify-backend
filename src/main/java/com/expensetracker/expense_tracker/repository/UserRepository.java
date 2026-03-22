package com.expensetracker.expense_tracker.repository;

import com.expensetracker.expense_tracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // 用 email 查找用户（登录时用）
    Optional<User> findByEmail(String email);

    // 检查 email 是否已被注册（注册时用）
    boolean existsByEmail(String email);
}