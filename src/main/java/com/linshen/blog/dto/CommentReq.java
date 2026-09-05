package com.linshen.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentReq {
    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称最长 50 字")
    private String author;
    @Size(max = 200, message = "邮箱最长 200 字")
    private String email;
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 2000, message = "评论最长 2000 字")
    private String content;
    private Long parentId;
}
