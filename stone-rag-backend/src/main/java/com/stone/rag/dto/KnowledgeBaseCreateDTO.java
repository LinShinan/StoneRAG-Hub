package com.stone.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "创建知识库请求")
public class KnowledgeBaseCreateDTO {

    @NotBlank(message = "名称不能为空")
    @Schema(description = "知识库名称", example = "工作文档")
    private String name;

    @Schema(description = "描述", example = "存放日常工作相关的技术文档")
    private String description;

    @Schema(description = "图标（emoji）", example = "📁")
    private String icon;

    @Schema(description = "嵌入模型", example = "text-embedding-3-small")
    private String embeddingModel;

    @Schema(description = "分段大小", example = "500")
    private Integer chunkSize;

    @Schema(description = "分段重叠", example = "50")
    private Integer chunkOverlap;
}