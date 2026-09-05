package com.linshen.blog.controller;

import com.linshen.blog.dto.ChangePasswordReq;
import com.linshen.blog.dto.LoginReq;
import com.linshen.blog.dto.LoginResp;
import com.linshen.blog.dto.Result;
import com.linshen.blog.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<LoginResp> login(@Valid @RequestBody LoginReq req) {
        return Result.ok(authService.login(req.getUsername(), req.getPassword()));
    }

    @GetMapping("/me")
    public Result<Map<String, String>> me(Authentication authentication) {
        return Result.ok(Map.of("username", authentication.getName()));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordReq req,
                                       Authentication authentication) {
        authService.changePassword(authentication.getName(),
                req.getOldPassword(), req.getNewPassword());
        return Result.ok(null);
    }
}
