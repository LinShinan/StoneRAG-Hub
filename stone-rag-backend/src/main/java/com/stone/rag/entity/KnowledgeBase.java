package com.stone.rag.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


@Data
@TableName("knowledge_bases")
public class KnowledgeBase extends BaseEntity{


    @TableField("user_id")
    private Long userId;

    private String name;

    private String description;

    private String icon;

    @TableField("embedding_model")
    private String embeddingModel;

    @TableField("chunk_size")
    private Integer chunkSize;

    @TableField("chunk_overlap")
    private Integer chunkOverlap;

    @TableField("doc_count")
    private Integer docCount;

    @TableField("total_chunks")
    private Integer totalChunks;

}
