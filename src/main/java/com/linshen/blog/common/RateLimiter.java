package com.linshen.blog.common;

import com.linshen.blog.dto.BizException;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/** 内存限频器：个人博客量级够用，key 为业务标识（如 "comment:IP"） */
@Component
public class RateLimiter {
    private final ConcurrentHashMap<String, Long> lastHits = new ConcurrentHashMap<>();

    public void check(String key, long intervalMillis) {
        long now = System.currentTimeMillis();
        Long last = lastHits.putIfAbsent(key, now);
        if (last != null) {
            if (now - last < intervalMillis) {
                throw new BizException(429, "操作太频繁，请稍后再试");
            }
            lastHits.put(key, now);
        }
    }

    /** 仅供测试隔离使用：内存状态不随 @Transactional 回滚 */
    public void reset() {
        lastHits.clear();
    }
}
