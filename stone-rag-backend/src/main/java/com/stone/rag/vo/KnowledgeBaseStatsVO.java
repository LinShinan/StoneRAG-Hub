package com.stone.rag.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "知识库统计信息")
public class KnowledgeBaseStatsVO {

    @Schema(description = "文档总数")
    private Integer docCount;

    @Schema(description = "切片总数")
    private Integer chunkCount;

    @Schema(description = "总字符数")
    private Long totalChars;

    @Schema(description = "文件类型分布，key=文件类型(pdf/md/url等)，value=数量")
    private Map<String, Integer> fileTypeDist;

    @Schema(description = "最近添加的文档(最多5条)")
    private List<RecentDoc> recentDocs;

    @Data
    @Schema(description = "最近文档")
    public static class RecentDoc {

        @Schema(description = "文档ID")
        private Long id;

        @Schema(description = "文档标题")
        private String title;

        @Schema(description = "创建时间")
        private LocalDateTime createdAt;
    }
}
