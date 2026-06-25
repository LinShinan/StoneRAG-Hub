package com.stone.rag.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // 1. 原有文档信息不动
        Info apiInfo = new Info()
                .title("Stone RAG Hub API")
                .version("1.0.0")
                .description("Stone RAG Hub 后端接口文档")
                .contact(new Contact().name("stone"));

        // 2. Token鉴权组件配置
        Components components = new Components()
                .addSecuritySchemes("BearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("格式：Bearer 你的token值")
                );

        // 3. 全局所有接口自动携带token请求头
        SecurityRequirement securityItem = new SecurityRequirement().addList("BearerAuth");

        return new OpenAPI()
                .info(apiInfo)
                .components(components)
                .addSecurityItem(securityItem);
    }
}