package com.linshen.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProjectReq {
    @NotBlank(message = "slug 不能为空")
    @Size(max = 200, message = "slug 长度不能超过 200")
    private String slug;
    @NotBlank(message = "名称不能为空")
    @Size(max = 200, message = "名称长度不能超过 200")
    private String name;
    @Size(max = 100, message = "角色长度不能超过 100")
    private String role;
    private Integer year;
    @Size(max = 500, message = "摘要长度不能超过 500")
    private String summary;
    private String content;
    @Size(max = 500, message = "封面地址长度不能超过 500")
    private String cover;
    @Size(max = 500, message = "封面地址长度不能超过 500")
    private String coverDark;
    @Size(max = 10, message = "首字母长度不能超过 10")
    private String letter;
    @Size(max = 500, message = "链接长度不能超过 500")
    private String link;
    private Boolean featured;
    private Integer sortOrder;
}
