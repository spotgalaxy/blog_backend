package com.linshen.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class PostReq {
    @NotBlank(message = "slug 不能为空")
    @Size(max = 200, message = "slug 长度不能超过 200")
    private String slug;
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过 200")
    private String title;
    @Size(max = 500, message = "摘要长度不能超过 500")
    private String summary;
    private String content;
    private List<String> tags;
    @Size(max = 500, message = "封面地址长度不能超过 500")
    private String cover;
    private Boolean draft;
    /** 可选：迁移历史数据时保留原发布日期；不传则发布时取当前时间 */
    private OffsetDateTime publishedAt;
}
