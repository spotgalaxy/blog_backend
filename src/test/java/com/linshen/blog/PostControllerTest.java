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
class PostControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper om;

    private static final String ADMIN_PASSWORD = "123456"; // 与 backend/.env 的 ADMIN_PASSWORD 一致

    private String loginToken() throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body).path("data").path("token").asText();
    }

    private long createPost(String slug, boolean draft) throws Exception {
        String body = mvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + loginToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"" + slug + "\",\"title\":\"T " + slug
                                + "\",\"summary\":\"s\",\"content\":\"# hi\\nbody\",\"tags\":[\"测试\"],\"draft\":"
                                + draft + "}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body).path("data").path("id").asLong();
    }

    private long createPostWithoutDraft(String slug) throws Exception {
        String body = mvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + loginToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"" + slug + "\",\"title\":\"T " + slug + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body).path("data").path("id").asLong();
    }

    @Test
    void anonymousList_onlySeesPublished() throws Exception {
        createPost("pub-post", false);
        createPost("draft-post", true);

        mvc.perform(get("/api/posts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[*].slug").value(org.hamcrest.Matchers.hasItem("pub-post")))
            .andExpect(jsonPath("$.data[*].slug").value(org.hamcrest.Matchers.not(
                    org.hamcrest.Matchers.hasItem("draft-post"))));
    }

    @Test
    void anonymousDraftDetail_returns404() throws Exception {
        createPost("hidden-post", true);
        mvc.perform(get("/api/posts/hidden-post"))
            .andExpect(status().isNotFound());
    }

    @Test
    void publishFlow_makesVisible() throws Exception {
        long id = createPost("flow-post", true);
        mvc.perform(get("/api/posts/flow-post")).andExpect(status().isNotFound());

        mvc.perform(post("/api/posts/" + id + "/publish")
                    .header("Authorization", "Bearer " + loginToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"draft\":false}"))
            .andExpect(status().isOk());

        mvc.perform(get("/api/posts/flow-post"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.draft").value(false));
    }

    @Test
    void updateAndDelete_requireAuth() throws Exception {
        long id = createPost("auth-post", false);
        mvc.perform(put("/api/posts/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"slug\":\"auth-post\",\"title\":\"new title\"}"))
            .andExpect(status().isUnauthorized());

        mvc.perform(delete("/api/posts/" + id))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateSlug_returns400() throws Exception {
        createPost("dup-post", false);
        mvc.perform(post("/api/posts")
                    .header("Authorization", "Bearer " + loginToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"slug\":\"dup-post\",\"title\":\"x\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createWithoutDraft_defaultsToDraft() throws Exception {
        createPostWithoutDraft("default-draft-post");
        // 管理员视角：可见草稿，且 draft=true
        mvc.perform(get("/api/posts/default-draft-post")
                    .header("Authorization", "Bearer " + loginToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.draft").value(true));
        // 匿名视角：草稿不可见
        mvc.perform(get("/api/posts/default-draft-post"))
            .andExpect(status().isNotFound());
    }

    @Test
    void paginatedList_returnsPageEnvelope() throws Exception {
        createPost("page-a", false);
        createPost("page-b", true);
        mvc.perform(get("/api/posts").param("page", "1").param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items").isArray())
            .andExpect(jsonPath("$.data.total").isNumber())
            .andExpect(jsonPath("$.data.page").value(1))
            .andExpect(jsonPath("$.data.size").value(1));
    }
}
