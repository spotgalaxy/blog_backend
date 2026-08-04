package com.linshen.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linshen.blog.dto.BizException;
import com.linshen.blog.dto.PageResult;
import com.linshen.blog.dto.PostReq;
import com.linshen.blog.dto.PostResp;
import com.linshen.blog.entity.Post;
import com.linshen.blog.mapper.PostMapper;
import com.linshen.blog.service.PostService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PostServiceImpl implements PostService {
    private final PostMapper postMapper;

    public PostServiceImpl(PostMapper postMapper) {
        this.postMapper = postMapper;
    }

    @Override
    public List<PostResp> list(String tag, boolean admin) {
        LambdaQueryWrapper<Post> w = new LambdaQueryWrapper<>();
        if (!admin) {
            w.eq(Post::getDraft, false);
        }
        if (tag != null && !tag.isBlank()) {
            w.apply("jsonb_exists(tags, {0})", tag);
        }
        w.orderByDesc(Post::getPublishedAt).orderByDesc(Post::getId);
        return postMapper.selectList(w).stream().map(this::toResp).toList();
    }

    @Override
    public PageResult<PostResp> list(String tag, boolean admin, long page, long size) {
        long current = Math.max(1, page);
        long limit = Math.min(Math.max(1, size), 100);
        LambdaQueryWrapper<Post> w = new LambdaQueryWrapper<>();
        if (!admin) {
            w.eq(Post::getDraft, false);
        }
        if (tag != null && !tag.isBlank()) {
            w.apply("jsonb_exists(tags, {0})", tag);
        }
        w.orderByDesc(Post::getPublishedAt).orderByDesc(Post::getId);
        Page<Post> p = postMapper.selectPage(new Page<>(current, limit), w);
        return new PageResult<>(
                p.getRecords().stream().map(this::toResp).toList(),
                p.getTotal(), current, limit);
    }

    @Override
    public PostResp getBySlug(String slug, boolean admin) {
        Post post = postMapper.selectOne(
                new LambdaQueryWrapper<Post>().eq(Post::getSlug, slug));
        if (post == null || (Boolean.TRUE.equals(post.getDraft()) && !admin)) {
            throw new BizException(404, "文章不存在");
        }
        return toResp(post);
    }

    @Override
    @Transactional
    public PostResp create(PostReq req) {
        if (postMapper.selectCount(
                new LambdaQueryWrapper<Post>().eq(Post::getSlug, req.getSlug())) > 0) {
            throw BizException.badRequest("slug 已存在: " + req.getSlug());
        }
        Post post = new Post();
        BeanUtils.copyProperties(req, post);
        if (post.getTags() == null) post.setTags(List.of());
        // 新建默认草稿：只有显式 draft=false 才发布
        if (post.getDraft() == null) {
            post.setDraft(true);
        }
        if (!Boolean.TRUE.equals(post.getDraft())) {
            if (post.getPublishedAt() == null) {
                post.setPublishedAt(OffsetDateTime.now());
            }
        }
        postMapper.insert(post);
        return toResp(post);
    }

    @Override
    @Transactional
    public PostResp update(Long id, PostReq req) {
        Post post = postMapper.selectById(id);
        if (post == null) throw new BizException(404, "文章不存在");
        if (!post.getSlug().equals(req.getSlug())
                && postMapper.selectCount(new LambdaQueryWrapper<Post>()
                        .eq(Post::getSlug, req.getSlug()).ne(Post::getId, id)) > 0) {
            throw BizException.badRequest("slug 已存在: " + req.getSlug());
        }
        // 发布时间不被请求覆盖：保留原值，草稿转发布时补当前时间
        OffsetDateTime publishedAt = post.getPublishedAt();
        BeanUtils.copyProperties(req, post);
        post.setPublishedAt(publishedAt);
        if (!Boolean.TRUE.equals(post.getDraft()) && post.getPublishedAt() == null) {
            post.setPublishedAt(OffsetDateTime.now());
        }
        postMapper.updateById(post);
        return toResp(post);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (postMapper.selectById(id) == null) throw new BizException(404, "文章不存在");
        postMapper.deleteById(id);
    }

    @Override
    @Transactional
    public PostResp publish(Long id, boolean published) {
        Post post = postMapper.selectById(id);
        if (post == null) throw new BizException(404, "文章不存在");
        post.setDraft(!published);
        if (published && post.getPublishedAt() == null) {
            post.setPublishedAt(OffsetDateTime.now());
        }
        postMapper.updateById(post);
        return toResp(post);
    }

    @Override
    public List<Map<String, Object>> tags() {
        return postMapper.selectTagCounts();
    }

    private PostResp toResp(Post post) {
        PostResp resp = new PostResp();
        BeanUtils.copyProperties(post, resp);
        return resp;
    }
}
