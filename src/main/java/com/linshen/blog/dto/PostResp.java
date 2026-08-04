package com.linshen.blog.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class PostResp {
    private Long id;
    private String slug;
    private String title;
    private String summary;
    private String content;
    private List<String> tags;
    private String cover;
    private Boolean draft;
    private OffsetDateTime publishedAt;
    private Long viewCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
