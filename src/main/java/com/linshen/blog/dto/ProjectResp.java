package com.linshen.blog.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ProjectResp {
    private Long id;
    private String slug;
    private String name;
    private String role;
    private String devStart;
    private String devEnd;
    private String summary;
    private String content;
    private String cover;
    private String coverDark;
    private String letter;
    private String link;
    private Boolean featured;
    private Integer sortOrder;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
