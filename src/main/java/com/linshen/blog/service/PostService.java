package com.linshen.blog.service;

import com.linshen.blog.dto.PostReq;
import com.linshen.blog.dto.PostResp;
import com.linshen.blog.dto.PageResult;

import java.util.List;
import java.util.Map;

public interface PostService {
    List<PostResp> list(String tag, boolean admin);
    PageResult<PostResp> list(String tag, boolean admin, long page, long size);
    PostResp getBySlug(String slug, boolean admin);
    PostResp create(PostReq req);
    PostResp update(Long id, PostReq req);
    void delete(Long id);
    PostResp publish(Long id, boolean published);
    List<Map<String, Object>> tags();

    /** 阅读量 +1；同日同 IP 同文章只计一次，返回本次是否计数 */
    boolean incrementView(String slug, String ip);
}
