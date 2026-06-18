package com.stone.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "更新知识库请求")
public class KnowledgeBaseUpdateDTO {

    @Schema(description = "知识库名称", example = "新名称")
    private String name;

    @Schema(description = "描述", example = "新描述")
    private String description;

    @Schema(description = "图标（emoji）", example = "📚")
    private String icon;
}
