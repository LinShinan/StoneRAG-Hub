package com.stone.rag.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "分页数据")
public class PageVO<T> {

    @Schema(description = "数据列表")
    private List<T> items;

    @Schema(description = "总条数")
    private long total;

    @Schema(description = "当前页码")
    private int page;

    @Schema(description = "每页大小")
    private int size;
}
