package com.linshen.blog.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 登录失败限流：按用户名/IP 维度，在时间窗口内失败次数超限则锁定该窗口。
 * 内存实现，适合单实例个人博客；多实例部署时应换 Redis。
 */
@Component
public class LoginRateLimiter {
    private static final class Entry {
        final AtomicInteger count = new AtomicInteger();
        volatile long windowStart;
    }

    private final ConcurrentHashMap<String, Entry> buckets = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final long windowSeconds;

    public LoginRateLimiter(@Value("${app.login.max-failures:5}") int maxFailures,
                            @Value("${app.login.window-seconds:900}") long windowSeconds) {
        this.maxFailures = maxFailures;
        this.windowSeconds = windowSeconds;
    }

    /** 返回剩余锁定秒数；0 表示未锁定 */
    public long lockRemainingSeconds(String key) {
        Entry e = buckets.get(key);
        if (e == null) return 0;
        long windowStart = e.windowStart;
        long elapsed = (System.currentTimeMillis() - windowStart) / 1000;
        if (elapsed >= windowSeconds) {
            buckets.remove(key);
            return 0;
        }
        return e.count.get() >= maxFailures ? (windowSeconds - elapsed) : 0;
    }

    /** 记录一次失败；若已超限则返回剩余锁定秒数（0 表示尚未锁定） */
    public long recordFailure(String key) {
        Entry e = buckets.computeIfAbsent(key, k -> {
            Entry ne = new Entry();
            ne.windowStart = System.currentTimeMillis();
            return ne;
        });
        long now = System.currentTimeMillis();
        long windowStart = e.windowStart;
        if (now - windowStart >= windowSeconds * 1000) {
            e.windowStart = now;
            e.count.set(0);
        }
        e.count.incrementAndGet();
        if (e.count.get() < maxFailures) return 0;
        long elapsed = (now - e.windowStart) / 1000;
        return Math.max(0, windowSeconds - elapsed);
    }

    /** 登录成功后清除失败记录 */
    public void reset(String key) {
        buckets.remove(key);
    }
}
