package com.stone.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    /** HMAC 密钥字符串，yaml 中 jwt.secret */
    private String secret;
    /** Token 有效期（毫秒），默认7天，yaml 中 jwt.expiration */
    private long expiration = 604800000;
}
