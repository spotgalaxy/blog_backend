package com.linshen.blog.service;

import com.linshen.blog.dto.ProjectReq;
import com.linshen.blog.dto.ProjectResp;

import java.util.List;

public interface ProjectService {
    List<ProjectResp> list();
    ProjectResp getBySlug(String slug);
    ProjectResp create(ProjectReq req);
    ProjectResp update(Long id, ProjectReq req);
    void delete(Long id);
}
