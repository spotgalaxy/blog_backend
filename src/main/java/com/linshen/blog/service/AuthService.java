package com.linshen.blog.service;

import com.linshen.blog.dto.LoginResp;

public interface AuthService {
    LoginResp login(String username, String password);
}
