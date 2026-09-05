package com.linshen.blog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjectControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper om;

    private static final String ADMIN_PASSWORD = "123456";

    private String token() throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body).path("data").path("token").asText();
    }

    private long createProject(String slug) throws Exception {
        String body = mvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"" + slug + "\",\"name\":\"项目 " + slug
                                + "\",\"role\":\"前端开发\",\"devStart\":\"2026-01\",\"devEnd\":\"2026-06\",\"summary\":\"s\",\"content\":\"body\",\"featured\":true,\"sortOrder\":1}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body).path("data").path("id").asLong();
    }

    @Test
    void anonymousList_returnsProjects() throws Exception {
        createProject("proj-a");
        // 库中已有真实作品（如 nova-ui-kit），断言"包含"而非"第一条"
        mvc.perform(get("/api/projects"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[*].slug").value(org.hamcrest.Matchers.hasItem("proj-a")))
            .andExpect(jsonPath("$.data[*].featured").value(org.hamcrest.Matchers.hasItem(true)));
    }

    @Test
    void getBySlug_returnsDetail() throws Exception {
        createProject("proj-b");
        mvc.perform(get("/api/projects/proj-b"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("项目 proj-b"));
    }

    @Test
    void writeOperations_requireAuth() throws Exception {
        mvc.perform(post("/api/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"slug\":\"x\",\"name\":\"x\"}"))
            .andExpect(status().isUnauthorized());

        long id = createProject("proj-c");
        mvc.perform(delete("/api/projects/" + id))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateSlug_returns400() throws Exception {
        createProject("dup-proj");
        mvc.perform(post("/api/projects")
                    .header("Authorization", "Bearer " + token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"slug\":\"dup-proj\",\"name\":\"x\"}"))
            .andExpect(status().isBadRequest());
    }
}
