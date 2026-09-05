package com.linshen.blog.controller;

import com.linshen.blog.dto.PostReq;
import com.linshen.blog.dto.PostResp;
import com.linshen.blog.dto.PageResult;
import com.linshen.blog.dto.Result;
import com.linshen.blog.service.AiSummaryService;
import com.linshen.blog.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;
    private final AiSummaryService aiSummaryService;

    public PostController(PostService postService, AiSummaryService aiSummaryService) {
        this.postService = postService;
        this.aiSummaryService = aiSummaryService;
    }

    @GetMapping
    public Result<Object> list(@RequestParam(required = false) String tag,
                               @RequestParam(required = false) Long page,
                               @RequestParam(required = false) Long size,
                               Authentication auth) {
        if (page != null || size != null) {
            return Result.ok(postService.list(tag, auth != null,
                    page == null ? 1 : page, size == null ? 10 : size));
        }
        return Result.ok(postService.list(tag, auth != null));
    }

    @GetMapping("/tags")
    public Result<List<Map<String, Object>>> tags() {
        return Result.ok(postService.tags());
    }

    @GetMapping("/{slug}")
    public Result<PostResp> get(@PathVariable String slug, Authentication auth) {
        return Result.ok(postService.getBySlug(slug, auth != null));
    }

    @PostMapping
    public Result<PostResp> create(@Valid @RequestBody PostReq req) {
        return Result.ok(postService.create(req));
    }

    @PutMapping("/{id}")
    public Result<PostResp> update(@PathVariable Long id, @Valid @RequestBody PostReq req) {
        return Result.ok(postService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        postService.delete(id);
        return Result.ok(null);
    }

    @PostMapping("/{id}/publish")
    public Result<PostResp> publish(@PathVariable Long id,
                                    @RequestBody Map<String, Boolean> body) {
        // 兼容旧字段 draft；优先使用 published，缺省时保持旧行为（draft=false 即发布）
        boolean published = Boolean.FALSE.equals(body.get("draft"));
        if (body.containsKey("published")) {
            published = Boolean.TRUE.equals(body.get("published"));
        }
        return Result.ok(postService.publish(id, published));
    }

    @PostMapping("/{slug}/view")
    public Result<Map<String, Object>> view(@PathVariable String slug,
                                            HttpServletRequest request) {
        boolean counted = postService.incrementView(slug, request.getRemoteAddr());
        return Result.ok(Map.of("counted", counted));
    }

    @GetMapping("/{slug}/summary")
    public Result<Map<String, Object>> summary(@PathVariable String slug) {
        return Result.ok(aiSummaryService.findBySlug(slug));
    }
}
