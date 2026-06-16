package com.stone.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("shares")
public class Share {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("kb_id")
    private Long kbId;

    @TableField("owner_id")
    private Long ownerId;

    @TableField("share_code")
    private String shareCode;

    private String permission;

    @TableField("is_active")
    private Boolean isActive;

    @TableField("expire_at")
    private LocalDateTime expireAt;

    @TableField("view_count")
    private Integer viewCount;

    @TableField("ask_count")
    private Integer askCount;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
