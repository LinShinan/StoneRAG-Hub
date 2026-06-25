package com.stone.rag.vo;

import lombok.Data;

import java.util.List;

@Data
public class AiParseResponse {

    private Integer docId;
    private List<ChunkItem> chunks;

    @Data
    public static class ChunkItem {
        private Integer index;
        private String text;
        private Integer charCount;
        private String hash;
    }
}
