package com.linshen.blog.controller;

import com.linshen.blog.dto.BizException;
import com.linshen.blog.dto.Result;
import com.linshen.blog.service.AiSummaryService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final AiSummaryService aiSummaryService;

    public AiController(AiSummaryService aiSummaryService) {
        this.aiSummaryService = aiSummaryService;
    }

    @PostMapping("/summary")
    public Result<Map<String, Object>> generate(@RequestBody Map<String, Long> body) {
        Long postId = body == null ? null : body.get("postId");
        if (postId == null) throw BizException.badRequest("postId 不能为空");
        return Result.ok(aiSummaryService.getOrCreate(postId));
    }

    @PostMapping("/project-intro")
    public Result<Map<String, Object>> generateProjectIntro(@RequestBody Map<String, Long> body) {
        Long projectId = body == null ? null : body.get("projectId");
        if (projectId == null) throw BizException.badRequest("projectId 不能为空");
        return Result.ok(Map.of("summary", aiSummaryService.generateProjectIntro(projectId)));
    }
}
