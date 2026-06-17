package com.stone.rag.dto;

import lombok.Data;

@Data
public class KnowledgeBaseCreateDTO {
    private String name;
    private String description;
    private String icon; // 可选，emoji 图标
    private String embeddingModel;
    private Integer chunkSize;
    private Integer chunkOverlap;
}