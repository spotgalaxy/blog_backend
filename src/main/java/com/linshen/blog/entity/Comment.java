package com.linshen.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("comments")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId;
    private Long parentId;
    private String author;
    private String email;
    private String content;
    private String status;
    private String ip;
    private String userAgent;
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
