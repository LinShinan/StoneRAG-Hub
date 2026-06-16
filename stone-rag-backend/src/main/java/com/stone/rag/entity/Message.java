package com.stone.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("messages")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("conversation_id")
    private Long conversationId;

    private String role;

    private String content;

    @TableField("sources_json")
    private String sourcesJson;

    @TableField("token_count")
    private Integer tokenCount;

    private String feedback;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
