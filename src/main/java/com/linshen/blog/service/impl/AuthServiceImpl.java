package com.linshen.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linshen.blog.dto.BizException;
import com.linshen.blog.dto.LoginResp;
import com.linshen.blog.entity.User;
import com.linshen.blog.mapper.UserMapper;
import com.linshen.blog.security.JwtUtil;
import com.linshen.blog.security.LoginRateLimiter;
import com.linshen.blog.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginRateLimiter rateLimiter;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil, LoginRateLimiter rateLimiter) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public LoginResp login(String username, String password) {
        String key = "login:" + username;
        long lockRemaining = rateLimiter.lockRemainingSeconds(key);
        if (lockRemaining > 0) {
            throw new BizException(429, "尝试次数过多，请 " + (lockRemaining / 60 + 1) + " 分钟后再试");
        }
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            rateLimiter.recordFailure(key);
            throw new BizException(401, "用户名或密码错误");
        }
        rateLimiter.reset(key);
        return new LoginResp(jwtUtil.generate(username), jwtUtil.getExpireHours() * 3600);
    }
}
