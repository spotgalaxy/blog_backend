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
class ViewStatsTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper om;

    private static final String ADMIN_PASSWORD = "123456"; // 与 backend/.env 的 ADMIN_PASSWORD 一致

    private String token() throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body).path("data").path("token").asText();
    }

    private void createPost(String slug) throws Exception {
        mvc.perform(post("/api/posts")
                    .header("Authorization", "Bearer " + token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"slug\":\"" + slug + "\",\"title\":\"t\",\"summary\":\"s\",\"content\":\"body\",\"tags\":[],\"draft\":false}"))
            .andExpect(status().isOk());
    }

    @Test
    void repeatedViews_sameIp_onlyCountedOnce() throws Exception {
        createPost("view-target");
        mvc.perform(post("/api/posts/view-target/view")).andExpect(status().isOk());
        mvc.perform(post("/api/posts/view-target/view")).andExpect(status().isOk());

        mvc.perform(get("/api/posts/view-target"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.viewCount").value(1));
    }

    @Test
    void viewOnMissingPost_returns404() throws Exception {
        mvc.perform(post("/api/posts/nope/view")).andExpect(status().isNotFound());
    }
}
