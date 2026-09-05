package com.linshen.blog.service;

import java.util.Map;

public interface AiSummaryService {
    /** 管理端：生成或取缓存（postId） */
    Map<String, Object> getOrCreate(Long postId);

    /** 前台：读缓存，未命中抛 404 */
    Map<String, Object> findBySlug(String slug);
}
