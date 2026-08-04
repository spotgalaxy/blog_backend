package com.linshen.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("projects")
public class Project {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String slug;
    private String name;
    private String role;
    private Integer year;
    private String summary;
    private String content;
    private String cover;
    private String coverDark;
    private String letter;
    private String link;
    private Boolean featured;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
