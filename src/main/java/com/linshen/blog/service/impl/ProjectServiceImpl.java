package com.linshen.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linshen.blog.dto.BizException;
import com.linshen.blog.dto.ProjectReq;
import com.linshen.blog.dto.ProjectResp;
import com.linshen.blog.entity.Project;
import com.linshen.blog.mapper.ProjectMapper;
import com.linshen.blog.service.ProjectService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectServiceImpl implements ProjectService {
    private final ProjectMapper projectMapper;

    public ProjectServiceImpl(ProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
    }

    @Override
    public List<ProjectResp> list() {
        return projectMapper.selectList(new LambdaQueryWrapper<Project>()
                .orderByDesc(Project::getFeatured)
                .orderByAsc(Project::getSortOrder)
                .orderByAsc(Project::getId)).stream().map(this::toResp).toList();
    }

    @Override
    public ProjectResp getBySlug(String slug) {
        Project p = projectMapper.selectOne(
                new LambdaQueryWrapper<Project>().eq(Project::getSlug, slug));
        if (p == null) throw new BizException(404, "作品不存在");
        return toResp(p);
    }

    @Override
    @Transactional
    public ProjectResp create(ProjectReq req) {
        if (projectMapper.selectCount(new LambdaQueryWrapper<Project>()
                .eq(Project::getSlug, req.getSlug())) > 0) {
            throw BizException.badRequest("slug 已存在: " + req.getSlug());
        }
        Project p = new Project();
        BeanUtils.copyProperties(req, p);
        projectMapper.insert(p);
        return toResp(p);
    }

    @Override
    @Transactional
    public ProjectResp update(Long id, ProjectReq req) {
        Project p = projectMapper.selectById(id);
        if (p == null) throw new BizException(404, "作品不存在");
        if (!p.getSlug().equals(req.getSlug())
                && projectMapper.selectCount(new LambdaQueryWrapper<Project>()
                        .eq(Project::getSlug, req.getSlug()).ne(Project::getId, id)) > 0) {
            throw BizException.badRequest("slug 已存在: " + req.getSlug());
        }
        BeanUtils.copyProperties(req, p);
        projectMapper.updateById(p);
        return toResp(p);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (projectMapper.selectById(id) == null) throw new BizException(404, "作品不存在");
        projectMapper.deleteById(id);
    }

    private ProjectResp toResp(Project p) {
        ProjectResp r = new ProjectResp();
        BeanUtils.copyProperties(p, r);
        return r;
    }
}
