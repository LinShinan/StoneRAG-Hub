package com.stone.rag.dto;

import lombok.Data;

@Data
public class AiParseRequest {
    private Integer docId;
    private String filePath;
    private String fileType;
}
