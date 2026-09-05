package com.linshen.blog.service;

import com.linshen.blog.dto.LoginResp;

public interface AuthService {
    LoginResp login(String username, String password);

    /** 修改密码：校验旧密码后更新哈希 */
    void changePassword(String username, String oldPassword, String newPassword);
}
