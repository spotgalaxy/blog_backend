package com.linshen.blog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StatsApiTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper om;
    @Autowired private com.linshen.blog.service.StatsService statsService;

    private static final String ADMIN_PASSWORD = "123456"; // 与 backend/.env 的 ADMIN_PASSWORD 一致

    private String token() throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body).path("data").path("token").asText();
    }

    @Test
    void trackThenAggregate_thenOverviewHasData() throws Exception {
        mvc.perform(post("/api/stats/track")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"path\":\"/blog/hello\"}"))
            .andExpect(status().isOk());

        // 手动触发聚合（等同定时任务）
        statsService.aggregate(java.time.LocalDate.now());

        mvc.perform(get("/api/stats/admin/overview?days=7")
                    .header("Authorization", "Bearer " + token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalPv").value(1));
    }

    @Test
    void overviewWithoutToken_returns401() throws Exception {
        mvc.perform(get("/api/stats/admin/overview?days=7"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void popular_returnsTopPosts() throws Exception {
        mvc.perform(post("/api/posts")
                    .header("Authorization", "Bearer " + token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"slug\":\"hot-post\",\"title\":\"hot\",\"summary\":\"s\",\"content\":\"b\",\"tags\":[],\"draft\":false}"))
            .andExpect(status().isOk());

        mvc.perform(get("/api/stats/admin/popular?limit=10")
                    .header("Authorization", "Bearer " + token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].slug").value("hot-post"))
            .andExpect(jsonPath("$.data[0].viewCount").value(0));
    }
}
