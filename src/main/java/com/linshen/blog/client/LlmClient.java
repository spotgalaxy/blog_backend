package com.linshen.blog.client;

import com.linshen.blog.dto.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** 调用 OpenAI 兼容接口（默认 DeepSeek）生成文章摘要 */
@Component
public class LlmClient {
    private final RestClient restClient;
    private final String baseUrl;
    private final String apiKey;
    private final String model;

    public LlmClient(@Value("${app.llm.base-url}") String baseUrl,
                     @Value("${app.llm.api-key}") String apiKey,
                     @Value("${app.llm.model}") String model) {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
        this.restClient = RestClient.builder().requestFactory(factory).build();
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    /** 生成 100-150 字中文摘要，返回摘要正文 */
    public String summarize(String title, String content) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BizException(400, "未配置 LLM_API_KEY");
        }
        String trimmed = content == null ? "" : content;
        if (trimmed.length() > 4000) trimmed = trimmed.substring(0, 4000);
        Map<String, Object> req = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system",
                                "content", "为给定文章生成 100-150 字中文摘要，只输出摘要正文，不要任何前缀或解释。"),
                        Map.of("role", "user",
                                "content", "标题：" + title + "\n\n正文：\n" + trimmed)),
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
