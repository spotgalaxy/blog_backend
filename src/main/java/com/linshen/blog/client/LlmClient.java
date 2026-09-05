package com.linshen.blog.client;

import com.linshen.blog.dto.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** 调用 OpenAI 兼容接口（默认 DeepSeek）生成文章摘要 / 项目一句话介绍 */
@Component
public class LlmClient {
    private final RestClient restClient;
    private final String baseUrl;
    private final String apiKey;
    private final String model;

    public LlmClient(@Value("${app.llm.base-url}") String baseUrl,
                     @Value("${app.llm.api-key}") String apiKey,
                     @Value("${app.llm.model}") String model) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
        this.restClient = RestClient.builder().requestFactory(factory).build();
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    /** 文章摘要：100-150 字中文摘要，返回摘要正文 */
    public String summarize(String title, String content) {
        String trimmed = truncate(content, 4000);
        return chat(List.of(
                Map.of("role", "system",
                        "content", "为给定文章生成 100-150 字中文摘要，只输出摘要正文，不要任何前缀或解释。"),
                Map.of("role", "user",
                        "content", "标题：" + title + "\n\n正文：\n" + trimmed)));
    }

    /** 项目一句话介绍：30-60 字，用于作品列表展示，返回介绍正文 */
    public String projectIntro(String name, String role, Integer year, String content) {
        StringBuilder input = new StringBuilder("项目名称：").append(name);
        if (role != null && !role.isBlank()) input.append("\n我的角色：").append(role);
        if (year != null) input.append("\n年份：").append(year);
        input.append("\n\n项目介绍正文：\n").append(truncate(content, 2000));
        return chat(List.of(
                Map.of("role", "system",
                        "content", "为给定项目生成 30-60 字的中文一句话介绍，突出项目定位与亮点，只输出介绍正文，不要任何前缀或解释。"),
                Map.of("role", "user",
                        "content", input.toString())));
    }

    private String truncate(String content, int max) {
        String trimmed = content == null ? "" : content;
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }

    private String chat(List<Map<String, String>> messages) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BizException(400, "未配置 LLM_API_KEY");
        }
        Map<String, Object> req = Map.of(
                "model", model,
                "messages", messages,
                "max_tokens", 300,
                "temperature", 0.3);
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = restClient.post()
                .uri(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(Map.class);
        if (resp == null) throw new BizException(500, "LLM 返回为空");
        List<?> choices = (List<?>) resp.get("choices");
        if (choices == null || choices.isEmpty()) throw new BizException(500, "LLM 无结果");
        Map<?, ?> first = (Map<?, ?>) choices.get(0);
        Map<?, ?> message = (Map<?, ?>) first.get("message");
        return (String) message.get("content");
    }

    public String getModel() {
        return model;
    }
}
