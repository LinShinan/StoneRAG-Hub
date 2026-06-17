package com.stone.rag.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("documents")
public class Document extends BaseEntity{

    @TableField("kb_id")
    private Long kbId;

    @TableField("user_id")
    private Long userId;

    private String title;

    @TableField("file_type")
    private String fileType;

    @TableField("file_size")
    private Long fileSize;

    @TableField("file_path")
    private String filePath;

    @TableField("source_url")
    private String sourceUrl;

    private String status;

    @TableField("chunk_count")
    private Integer chunkCount;

    @TableField("error_msg")
    private String errorMsg;

    @TableField("tags_json")
    private String tagsJson;

}
