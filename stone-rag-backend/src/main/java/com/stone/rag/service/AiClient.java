package com.stone.rag.service;

import com.stone.rag.common.Result;
import com.stone.rag.dto.AiParseRequest;
import com.stone.rag.vo.AiParseResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class AiClient {

    private final WebClient webClient;

    public AiClient(WebClient.Builder builder, @Value("${ai-service.base-url}") String baseUrl){
        this.webClient=builder.baseUrl(baseUrl).build();
    }

    /**
     * 调用Python rag/parse解析文档
     * @param request
     * @return
     */
    public AiParseResponse parse(AiParseRequest request){
        //1. 向Python发送请求
        Result<AiParseResponse> result = webClient.post()
                .uri("/rag/parse")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Result<AiParseResponse>>() {
                }).block();
        //2. 校验返回结果
        if (result == null || result.getCode() != 200) {
            String msg = result != null ? result.getMessage() : "AI 服务无响应";
            throw new RuntimeException("文档解析失败: " + msg);
        }
        // 3. 返回解析后的 chunks
        return result.getData();
    }

}
