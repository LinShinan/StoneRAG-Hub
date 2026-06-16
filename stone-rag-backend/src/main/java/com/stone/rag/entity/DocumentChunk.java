package com.stone.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("document_chunks")
public class DocumentChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("document_id")
    private Long documentId;

    @TableField("kb_id")
    private Long kbId;

    @TableField("chunk_index")
    private Integer chunkIndex;

    @TableField("chunk_hash")
    private String chunkHash;

    @TableField("content_text")
    private String contentText;

    @TableField("char_count")
    private Integer charCount;

    @TableField("chroma_id")
    private String chromaId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
