package com.linshen.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linshen.blog.common.RateLimiter;
import com.linshen.blog.dto.BizException;
import com.linshen.blog.dto.CommentReq;
import com.linshen.blog.dto.CommentResp;
import com.linshen.blog.entity.Comment;
import com.linshen.blog.entity.Post;
import com.linshen.blog.mapper.CommentMapper;
import com.linshen.blog.mapper.PostMapper;
import com.linshen.blog.service.CommentService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommentServiceImpl implements CommentService {
    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final RateLimiter rateLimiter;

    public CommentServiceImpl(CommentMapper commentMapper, PostMapper postMapper,
                              RateLimiter rateLimiter) {
        this.commentMapper = commentMapper;
        this.postMapper = postMapper;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public List<CommentResp> listBySlug(String slug) {
        Post post = postMapper.selectOne(
                new LambdaQueryWrapper<Post>().eq(Post::getSlug, slug));
        if (post == null) throw new BizException(404, "文章不存在");
        List<Comment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getPostId, post.getId())
                        .eq(Comment::getStatus, "approved")
                        .orderByAsc(Comment::getCreatedAt)
                        .orderByAsc(Comment::getId));
        // 组装树：根评论 + replies
        Map<Long, CommentResp> byId = new HashMap<>();
        List<CommentResp> roots = new ArrayList<>();
        for (Comment c : comments) {
            CommentResp resp = new CommentResp();
            resp.setId(c.getId());
            resp.setParentId(c.getParentId());
            resp.setAuthor(c.getAuthor());
            resp.setContent(c.getContent());
            resp.setCreatedAt(c.getCreatedAt());
            byId.put(c.getId(), resp);
            if (c.getParentId() == null) {
                roots.add(resp);
            }
        }
        for (Comment c : comments) {
            if (c.getParentId() != null) {
                CommentResp parent = byId.get(c.getParentId());
                if (parent != null) parent.getReplies().add(byId.get(c.getId()));
            }
        }
        return roots;
    }

    @Override
    public CommentResp submit(String slug, CommentReq req, String ip, String ua) {
        rateLimiter.check("comment:" + ip, 10_000);
        Post post = postMapper.selectOne(
                new LambdaQueryWrapper<Post>().eq(Post::getSlug, slug));
        if (post == null) throw new BizException(404, "文章不存在");
        if (Boolean.TRUE.equals(post.getDraft())) throw new BizException(404, "文章不存在");
        Comment c = new Comment();
        c.setPostId(post.getId());
        c.setParentId(req.getParentId());
        c.setAuthor(req.getAuthor());
        c.setEmail(req.getEmail());
        c.setContent(req.getContent());
        c.setStatus("pending");
        c.setIp(ip);
        c.setUserAgent(ua != null && ua.length() > 500 ? ua.substring(0, 500) : ua);
        commentMapper.insert(c);
        CommentResp resp = new CommentResp();
        resp.setId(c.getId());
        resp.setAuthor(c.getAuthor());
        resp.setContent(c.getContent());
        resp.setCreatedAt(c.getCreatedAt());
        return resp;
    }
}
