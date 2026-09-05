package com.linshen.blog.service;

import com.linshen.blog.dto.CommentReq;
import com.linshen.blog.dto.CommentResp;

import java.util.List;

public interface CommentService {
    /** 前台读取：仅已通过审核的评论树 */
    List<CommentResp> listBySlug(String slug);

    /** 前台提交：写入 pending，等待审核 */
    CommentResp submit(String slug, CommentReq req, String ip, String ua);
}
