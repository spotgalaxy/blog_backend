package com.linshen.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linshen.blog.dto.BizException;
import com.linshen.blog.dto.Result;
import com.linshen.blog.entity.Comment;
import com.linshen.blog.entity.Post;
import com.linshen.blog.mapper.CommentMapper;
import com.linshen.blog.mapper.PostMapper;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/comments")
public class AdminCommentController {
    private final CommentMapper commentMapper;
    private final PostMapper postMapper;

    public AdminCommentController(CommentMapper commentMapper, PostMapper postMapper) {
        this.commentMapper = commentMapper;
        this.postMapper = postMapper;
    }

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize) {
        Page<Comment> p = commentMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<Comment>()
                        .eq(status != null && !status.isBlank(), Comment::getStatus, status)
                        .orderByDesc(Comment::getCreatedAt));
        Map<Long, String> slugById = postMapper.selectBatchIds(
                        p.getRecords().stream().map(Comment::getPostId).toList())
                .stream().collect(Collectors.toMap(Post::getId, Post::getSlug));
        List<Map<String, Object>> records = p.getRecords().stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("postId", c.getPostId());
            m.put("postSlug", slugById.get(c.getPostId()));
            m.put("author", c.getAuthor());
            m.put("content", c.getContent());
            m.put("status", c.getStatus());
            m.put("createdAt", c.getCreatedAt());
            return m;
        }).toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", records);
        data.put("total", p.getTotal());
        return Result.ok(data);
    }

    @PutMapping("/{id}")
    public Result<Void> review(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Comment c = commentMapper.selectById(id);
        if (c == null) throw new BizException(404, "评论不存在");
        String status = body.get("status");
        if (!"approved".equals(status) && !"rejected".equals(status)) {
            throw BizException.badRequest("status 仅支持 approved / rejected");
        }
        c.setStatus(status);
        commentMapper.updateById(c);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (commentMapper.selectById(id) == null) throw new BizException(404, "评论不存在");
        commentMapper.deleteById(id);
        return Result.ok(null);
    }
}
