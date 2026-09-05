package com.linshen.blog.controller;

import com.linshen.blog.dto.CommentReq;
import com.linshen.blog.dto.CommentResp;
import com.linshen.blog.dto.Result;
import com.linshen.blog.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts/{slug}/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public Result<List<CommentResp>> list(@PathVariable String slug) {
        return Result.ok(commentService.listBySlug(slug));
    }

    @PostMapping
    public Result<CommentResp> submit(@PathVariable String slug,
                                      @Valid @RequestBody CommentReq req,
                                      HttpServletRequest request) {
        return Result.ok(commentService.submit(slug, req,
                request.getRemoteAddr(), request.getHeader("User-Agent")));
    }
}
