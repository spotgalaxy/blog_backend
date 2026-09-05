package com.linshen.blog.controller;

import com.linshen.blog.dto.Result;
import com.linshen.blog.service.StatsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {
    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @PostMapping("/track")
    public Result<Void> track(@RequestBody(required = false) Map<String, String> body,
                              HttpServletRequest request) {
        statsService.track(
                body == null ? null : body.get("path"),
                request.getRemoteAddr(),
                request.getHeader("User-Agent"),
                body == null ? null : body.get("referer"));
        return Result.ok(null);
    }

    @GetMapping("/admin/overview")
    public Result<Map<String, Object>> overview(@RequestParam(defaultValue = "30") int days) {
        return Result.ok(statsService.overview(Math.min(Math.max(1, days), 365)));
    }

    @GetMapping("/admin/popular")
    public Result<List<Map<String, Object>>> popular(
            @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(statsService.popular(Math.min(Math.max(1, limit), 50)));
    }
}
