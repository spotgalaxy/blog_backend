package com.linshen.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("visit_log")
public class VisitLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String path;
    private String ip;
    private String ua;
    private String referer;
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
