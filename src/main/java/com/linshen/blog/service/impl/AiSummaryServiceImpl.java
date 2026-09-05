package com.linshen.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linshen.blog.client.LlmClient;
import com.linshen.blog.dto.BizException;
import com.linshen.blog.entity.AiSummary;
import com.linshen.blog.entity.Post;
import com.linshen.blog.entity.Project;
import com.linshen.blog.mapper.AiSummaryMapper;
import com.linshen.blog.mapper.PostMapper;
import com.linshen.blog.mapper.ProjectMapper;
import com.linshen.blog.service.AiSummaryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AiSummaryServiceImpl implements AiSummaryService {
    private final AiSummaryMapper aiSummaryMapper;
    private final PostMapper postMapper;
    private final ProjectMapper projectMapper;
    private final LlmClient llmClient;

    public AiSummaryServiceImpl(AiSummaryMapper aiSummaryMapper, PostMapper postMapper,
                                ProjectMapper projectMapper, LlmClient llmClient) {
        this.aiSummaryMapper = aiSummaryMapper;
        this.postMapper = postMapper;
        this.projectMapper = projectMapper;
        this.llmClient = llmClient;
    }

    @Override
    @Transactional
    public Map<String, Object> getOrCreate(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) throw new BizException(404, "文章不存在");
        AiSummary existing = aiSummaryMapper.selectOne(
                new LambdaQueryWrapper<AiSummary>().eq(AiSummary::getPostId, postId));
        if (existing != null) return toMap(existing);

        String summary = llmClient.summarize(post.getTitle(), post.getContent());
        AiSummary s = new AiSummary();
        s.setPostId(postId);
        s.setSummary(summary);
        s.setModel(llmClient.getModel());
        aiSummaryMapper.insert(s);
        return toMap(s);
    }

    @Override
    public Map<String, Object> findBySlug(String slug) {
        Post post = postMapper.selectOne(
                new LambdaQueryWrapper<Post>().eq(Post::getSlug, slug));
        if (post == null) throw new BizException(404, "文章不存在");
        AiSummary s = aiSummaryMapper.selectOne(
                new LambdaQueryWrapper<AiSummary>().eq(AiSummary::getPostId, post.getId()));
        if (s == null) throw new BizException(404, "暂无摘要");
        return toMap(s);
    }

    @Override
    public String generateProjectIntro(Long projectId) {
        Project p = projectMapper.selectById(projectId);
        if (p == null) throw new BizException(404, "作品不存在");
        return llmClient.projectIntro(p.getName(), p.getRole(), p.getDevStart(), p.getDevEnd(), p.getContent());
    }

    private Map<String, Object> toMap(AiSummary s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("summary", s.getSummary());
        m.put("model", s.getModel());
        m.put("updatedAt", s.getUpdatedAt());
        return m;
    }
}
