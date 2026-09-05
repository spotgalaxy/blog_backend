package com.linshen.blog.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface StatsService {
    /** 前台上报：写入 visit_log */
    void track(String path, String ip, String ua, String referer);

    /** 聚合某日 visit_log → daily_stats（幂等） */
    void aggregate(LocalDate date);

    /** 最近 N 天 PV/UV 序列（缺失日期补 0） */
    Map<String, Object> overview(int days);

    /** 热门文章 Top N（按 view_count） */
    List<Map<String, Object>> popular(int limit);
}
