package com.linshen.blog.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CommentResp {
    private Long id;
    private Long parentId;
    private String author;
    private String content;
    private OffsetDateTime createdAt;
    private List<CommentResp> replies = new ArrayList<>();
}
