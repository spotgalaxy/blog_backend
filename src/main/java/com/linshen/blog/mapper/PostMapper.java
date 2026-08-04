package com.linshen.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linshen.blog.entity.Post;
import org.apache.ibatis.annotations.Select;

public interface PostMapper extends BaseMapper<Post> {

    /** 标签聚合统计：在数据库侧展开 jsonb 数组并计数，避免全表加载到内存 */
    @Select("""
            SELECT t AS name, count(*) AS count
            FROM posts, jsonb_array_elements_text(tags) AS t
            WHERE draft = false
            GROUP BY t
            ORDER BY count(*) DESC, t ASC
            """)
    java.util.List<java.util.Map<String, Object>> selectTagCounts();
}
