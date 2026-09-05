package com.linshen.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@TableName("daily_stats")
public class DailyStat {
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate statDate;
    private Long pv;
    private Long uv;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
