package com.stone.rag.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversations")
public class Conversation extends BaseEntity{

    @TableField("user_id")
    private Long userId;

    @TableField("kb_id")
    private Long kbId;

    @TableField("redis_key_suffix")
    private String redisKeySuffix;

    private String title;

    private String model;

    @TableField("message_count")
    private Integer messageCount;

}
