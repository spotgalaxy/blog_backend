package com.linshen.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linshen.blog.entity.DailyStat;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

public interface DailyStatMapper extends BaseMapper<DailyStat> {

    /** 聚合某日 visit_log → daily_stats（幂等，可重复执行） */
    @Insert("""
            INSERT INTO daily_stats (stat_date, pv, uv, updated_at)
            SELECT #{date}::date, COUNT(*), COUNT(DISTINCT ip), NOW()
            FROM visit_log
            WHERE created_at >= #{date}::date
              AND created_at < #{date}::date + INTERVAL '1 day'
            ON CONFLICT (stat_date)
            DO UPDATE SET pv = EXCLUDED.pv, uv = EXCLUDED.uv, updated_at = NOW()
            """)
    int aggregateDate(@Param("date") LocalDate date);
}
