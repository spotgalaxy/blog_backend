package com.linshen.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PageResult<T> {
    private List<T> items;
    private long total;
    private long page;
    private long size;
}
