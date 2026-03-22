package com.expensetracker.expense_tracker.service;

import com.expensetracker.expense_tracker.dto.LoginRequest;
import com.expensetracker.expense_tracker.dto.RegisterRequest;
import com.expensetracker.expense_tracker.model.User;
import com.expensetracker.expense_tracker.repository.UserRepository;
import com.expensetracker.expense_tracker.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public String register(RegisterRequest req) {
        // 1. 检查邮箱有没有被注册过
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // 2. 把密码加密后存进数据库（不能存明文！）
        User user = User.builder()
            .email(req.getEmail())
            .password(encoder.encode(req.getPassword()))
            .name(req.getName())
            .build();

        userRepo.save(user);

        // 3. 注册成功后直接返回 token
        return jwtUtil.generateToken(user.getEmail());
    }

    public String login(LoginRequest req) {
        // 1. 用 email 找用户
        User user = userRepo.findByEmail(req.getEmail())
            .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. 验证密码（对比加密后的密码）
        if (!encoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // 3. 密码正确，返回 token
        return jwtUtil.generateToken(user.getEmail());
    }
}