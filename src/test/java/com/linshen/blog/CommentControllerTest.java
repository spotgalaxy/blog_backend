package com.linshen.blog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linshen.blog.common.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 测试直接运行在 blog 库（无独立测试库）；@Transactional 保证
 * 每个测试方法执行后回滚，不残留数据。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommentControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper om;
    @Autowired private RateLimiter rateLimiter;

    private static final String ADMIN_PASSWORD = "123456"; // 与 backend/.env 的 ADMIN_PASSWORD 一致

    @BeforeEach
    void resetRateLimiter() {
        rateLimiter.reset();
    }

    private String token() throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body).path("data").path("token").asText();
    }

    private long createPublishedPost(String slug) throws Exception {
        String body = mvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"" + slug + "\",\"title\":\"t\",\"summary\":\"s\",\"content\":\"body\",\"tags\":[],\"draft\":false}"))
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body).path("data").path("id").asLong();
    }

    private void submitComment(String slug, String author, String content) throws Exception {
        mvc.perform(post("/api/posts/" + slug + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"author\":\"" + author + "\",\"content\":\"" + content + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void submitThenPending_hiddenFromPublic() throws Exception {
        createPublishedPost("comment-target");
        submitComment("comment-target", "路人", "写得好");

        // 未审核：前台列表为空
        mvc.perform(get("/api/posts/comment-target/comments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void pendingComment_visibleAfterApprove() throws Exception {
        createPublishedPost("comment-target");
        submitComment("comment-target", "路人", "写得好");

        // admin 审核通过
        String listBody = mvc.perform(get("/api/admin/comments?status=pending")
                        .header("Authorization", "Bearer " + token()))
                .andReturn().getResponse().getContentAsString();
        long id = om.readTree(listBody).path("data").path("records").get(0).path("id").asLong();

        mvc.perform(put("/api/admin/comments/" + id)
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"approved\"}"))
            .andExpect(status().isOk());

        mvc.perform(get("/api/posts/comment-target/comments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].author").value("路人"));
    }

    @Test
    void commentOnMissingPost_returns404() throws Exception {
        mvc.perform(post("/api/posts/nonexistent/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"author\":\"a\",\"content\":\"b\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void blankContent_rejected() throws Exception {
        createPublishedPost("comment-target");
        mvc.perform(post("/api/posts/comment-target/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"author\":\"a\",\"content\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void replyToApprovedComment_showsInTree() throws Exception {
        createPublishedPost("comment-target");
        submitComment("comment-target", "路人", "写得好");

        String listBody = mvc.perform(get("/api/admin/comments?status=pending")
                        .header("Authorization", "Bearer " + token()))
                .andReturn().getResponse().getContentAsString();
        long rootId = om.readTree(listBody).path("data").path("records").get(0).path("id").asLong();
        mvc.perform(put("/api/admin/comments/" + rootId)
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"approved\"}"))
            .andExpect(status().isOk());

        // 楼中楼回复（模拟距上次提交已过限频窗口）
        rateLimiter.reset();
        mvc.perform(post("/api/posts/comment-target/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"author\":\"楼主\",\"content\":\"谢谢\",\"parentId\":" + rootId + "}"))
            .andExpect(status().isOk());
        String approveBody = mvc.perform(get("/api/admin/comments?status=pending")
                        .header("Authorization", "Bearer " + token()))
                .andReturn().getResponse().getContentAsString();
        long replyId = om.readTree(approveBody).path("data").path("records").get(0).path("id").asLong();
        mvc.perform(put("/api/admin/comments/" + replyId)
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"approved\"}"))
            .andExpect(status().isOk());

        mvc.perform(get("/api/posts/comment-target/comments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].replies[0].author").value("楼主"));
    }
}
