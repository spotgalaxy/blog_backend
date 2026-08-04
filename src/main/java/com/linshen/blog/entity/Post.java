package com.linshen.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.linshen.blog.mybatis.JsonbTypeHandler;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@TableName(value = "posts", autoResultMap = true)
public class Post {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String slug;
    private String title;
    private String summary;
    private String content;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private List<String> tags;
    private String cover;
    private Boolean draft;
    private OffsetDateTime publishedAt;
    private Long viewCount;
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
