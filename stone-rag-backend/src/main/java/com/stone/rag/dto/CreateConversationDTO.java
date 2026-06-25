package com.stone.rag.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateConversationDTO {
    @NotNull
    private Long kbId;
    private String title;
}
