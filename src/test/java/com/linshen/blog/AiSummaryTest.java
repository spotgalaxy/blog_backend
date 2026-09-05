package com.linshen.blog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linshen.blog.client.LlmClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AiSummaryTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper om;
    @MockitoBean private LlmClient llmClient;

    private static final String ADMIN_PASSWORD = "123456"; // 与 backend/.env 的 ADMIN_PASSWORD 一致

    private String token() throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body).path("data").path("token").asText();
    }

    private long createPost(String slug) throws Exception {
        String body = mvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"" + slug + "\",\"title\":\"t\",\"summary\":\"s\",\"content\":\"# hi\\nbody\",\"tags\":[],\"draft\":false}"))
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body).path("data").path("id").asLong();
    }

    @Test
    void generate_thenPublicRead_hitCache() throws Exception {
        when(llmClient.summarize(anyString(), anyString()))
                .thenReturn("这是一段 AI 生成的摘要。");
        when(llmClient.getModel()).thenReturn("deepseek-chat");

        long id = createPost("ai-post");
        mvc.perform(post("/api/ai/summary")
                    .header("Authorization", "Bearer " + token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"postId\":" + id + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.summary").value("这是一段 AI 生成的摘要。"));

        // 前台命中缓存（不再调用 LLM）
        mvc.perform(get("/api/posts/ai-post/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.summary").value("这是一段 AI 生成的摘要。"));

        // 验证只调用了一次
        verify(llmClient, times(1)).summarize(anyString(), anyString());
    }

    @Test
    void publicRead_withoutCache_returns404() throws Exception {
        createPost("no-summary");
        mvc.perform(get("/api/posts/no-summary/summary"))
            .andExpect(status().isNotFound());
    }

    @Test
    void generateWithoutKey_returns400() throws Exception {
        when(llmClient.summarize(anyString(), anyString()))
                .thenThrow(new com.linshen.blog.dto.BizException(400, "未配置 LLM_API_KEY"));
        long id = createPost("no-key-post");
        mvc.perform(post("/api/ai/summary")
                    .header("Authorization", "Bearer " + token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"postId\":" + id + "}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void projectIntro_generatedAndRequiresAuth() throws Exception {
        when(llmClient.projectIntro(anyString(), anyString(), any(), any(), anyString()))
                .thenReturn("一个暖色调的个人博客站点。");
        String body = mvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"intro-proj\",\"name\":\"测试作品\",\"role\":\"前端开发\",\"devStart\":\"2026-01\",\"devEnd\":\"2026-06\",\"summary\":\"\",\"content\":\"一个博客项目\",\"featured\":false,\"sortOrder\":99}"))
                .andReturn().getResponse().getContentAsString();
        long id = om.readTree(body).path("data").path("id").asLong();

        // 未带 token 拒绝
        mvc.perform(post("/api/ai/project-intro")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"projectId\":" + id + "}"))
            .andExpect(status().isUnauthorized());

        // 管理端生成
        mvc.perform(post("/api/ai/project-intro")
                    .header("Authorization", "Bearer " + token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"projectId\":" + id + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.summary").value("一个暖色调的个人博客站点。"));
    }
}
