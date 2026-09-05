package com.linshen.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linshen.blog.entity.DailyStat;
import com.linshen.blog.entity.Post;
import com.linshen.blog.entity.VisitLog;
import com.linshen.blog.mapper.DailyStatMapper;
import com.linshen.blog.mapper.PostMapper;
import com.linshen.blog.mapper.VisitLogMapper;
import com.linshen.blog.service.StatsService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class StatsServiceImpl implements StatsService {
    private final VisitLogMapper visitLogMapper;
    private final DailyStatMapper dailyStatMapper;
    private final PostMapper postMapper;

    public StatsServiceImpl(VisitLogMapper visitLogMapper, DailyStatMapper dailyStatMapper,
                            PostMapper postMapper) {
        this.visitLogMapper = visitLogMapper;
        this.dailyStatMapper = dailyStatMapper;
        this.postMapper = postMapper;
    }

    @Override
    public void track(String path, String ip, String ua, String referer) {
        VisitLog v = new VisitLog();
        v.setPath(path != null && path.length() > 500 ? path.substring(0, 500) : path);
        v.setIp(ip);
        v.setUa(ua != null && ua.length() > 500 ? ua.substring(0, 500) : ua);
        v.setReferer(referer != null && referer.length() > 500 ? referer.substring(0, 500) : referer);
        visitLogMapper.insert(v);
    }

    /** 每小时由定时任务重算当日 pv/uv（延迟聚合，看板只读 daily_stats） */
    @Scheduled(cron = "0 0 * * * *")
    public void aggregateNow() {
        aggregate(LocalDate.now());
    }

    @Override
    public void aggregate(LocalDate date) {
        dailyStatMapper.aggregateDate(date);
    }

    @Override
    public Map<String, Object> overview(int days) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(days - 1L);
        List<DailyStat> stats = dailyStatMapper.selectList(
                new LambdaQueryWrapper<DailyStat>()
                        .ge(DailyStat::getStatDate, start)
                        .le(DailyStat::getStatDate, today)
                        .orderByAsc(DailyStat::getStatDate));
        Map<LocalDate, DailyStat> byDate = new HashMap<>();
        for (DailyStat s : stats) byDate.put(s.getStatDate(), s);

        List<Map<String, Object>> dayList = new ArrayList<>();
        long totalPv = 0, totalUv = 0;
        for (LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)) {
            DailyStat s = byDate.get(d);
            long pv = s == null ? 0 : s.getPv();
            long uv = s == null ? 0 : s.getUv();
            totalPv += pv;
            totalUv += uv;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", d.toString());
            m.put("pv", pv);
            m.put("uv", uv);
            dayList.add(m);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("days", dayList);
        result.put("totalPv", totalPv);
        result.put("totalUv", totalUv);
        return result;
    }

    @Override
    public List<Map<String, Object>> popular(int limit) {
        return postMapper.selectList(new LambdaQueryWrapper<Post>()
                        .eq(Post::getDraft, false)
                        .orderByDesc(Post::getViewCount)
                        .orderByDesc(Post::getId)
                        .last("LIMIT " + Math.max(1, limit)))
                .stream().map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("slug", p.getSlug());
                    m.put("title", p.getTitle());
                    m.put("viewCount", p.getViewCount());
                    return m;
                }).toList();
    }
}
