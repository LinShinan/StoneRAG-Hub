package com.stone.rag.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KnowledgeBaseVO {
    private Long id;
    private Long userId;
    private String name;
    private String description;
    private String icon;
    private String embeddingModel;
    private Integer chunkSize;
    private Integer chunkOverlap;
    private Integer docCount;
    private Integer totalChunks;

//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8") //已经在配置文件中配置
    private LocalDateTime createdAt;

//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}