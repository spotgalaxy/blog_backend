package com.linshen.blog.init;

import com.linshen.blog.entity.User;
import com.linshen.blog.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;
    @Value("${app.admin.password}")
    private String adminPassword;

    public DataInitializer(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        User existing = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, adminUsername));
        if (existing == null) {
            User user = new User();
            user.setUsername(adminUsername);
            user.setPasswordHash(passwordEncoder.encode(adminPassword));
            userMapper.insert(user);
            log.info("Initialized admin user: {}", adminUsername);
        } else {
            // 管理员已存在：不覆盖密码，避免重启时重置管理员自行修改的密码
            log.info("Admin user '{}' already exists, skip initialization", adminUsername);
        }
    }
}
