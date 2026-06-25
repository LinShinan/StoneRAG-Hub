package com.stone.rag.vo;

import lombok.Data;
import java.util.List;

@Data
public class DocumentDetailVO {
    private Long id;
    private String title;
    private String fileType;
    private Long fileSize;
    private String status;
    private Integer chunkCount;
    private String errorMsg;
    private List<String> tagsJson;
    private List<ChunkPreview> chunksPreview;

    @Data
    public static class ChunkPreview {
        private Integer index;
        private String contentText;
        private Integer charCount;
    }
}
