package com.linshen.blog.controller;

import com.linshen.blog.dto.ProjectReq;
import com.linshen.blog.dto.ProjectResp;
import com.linshen.blog.dto.Result;
import com.linshen.blog.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public Result<List<ProjectResp>> list() {
        return Result.ok(projectService.list());
    }

    @GetMapping("/{slug}")
    public Result<ProjectResp> get(@PathVariable String slug) {
        return Result.ok(projectService.getBySlug(slug));
    }

    @PostMapping
    public Result<ProjectResp> create(@Valid @RequestBody ProjectReq req) {
        return Result.ok(projectService.create(req));
    }

    @PutMapping("/{id}")
    public Result<ProjectResp> update(@PathVariable Long id, @Valid @RequestBody ProjectReq req) {
        return Result.ok(projectService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return Result.ok(null);
    }
}
