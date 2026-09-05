package com.linshen.blog.service;

import java.util.Map;

public interface AiSummaryService {
    /** 管理端：生成或取缓存（postId） */
    Map<String, Object> getOrCreate(Long postId);

    /** 前台：读缓存，未命中抛 404 */
    Map<String, Object> findBySlug(String slug);

    /** 管理端：为作品生成一句话介绍（不落库，填回表单由用户保存） */
    String generateProjectIntro(Long projectId);
}
