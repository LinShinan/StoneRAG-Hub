package com.stone.rag.controller;

import com.stone.rag.common.Result;
import com.stone.rag.service.DocumentService;
import com.stone.rag.vo.DocumentDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name="文档管理")
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/documents")
@RestController
public class DocumentController {

    private final DocumentService documentService;

    /**
     * 文档详情
     * @param id
     * @return
     */
    @Operation(summary="文档详情")
    @GetMapping("/{id}")
    public Result<DocumentDetailVO> getDetail(@PathVariable Long id){
        DocumentDetailVO vo = documentService.getDetail(id);
        log.info("获取文档详情成功：{}", vo);
        return Result.success(vo);
    }

    /**
     * 删除文档
     * @param id
     * @return
     */
    @Operation(summary="删除文档")
    @DeleteMapping("/{id}")
    public Result<Void> deleteDocument(@PathVariable Long id){
        documentService.deleteDocument(id);
        log.info("删除id={}文档成功", id);
        return Result.success();
    }

    /**
     * 重新处理失败文档
     * @param id
     * @return
     */
    @Operation(summary = "重新处理失败文档")
    @PostMapping("/{id}/reprocess")
    public Result<Void> reprocess(@PathVariable Long id) {
        documentService.reprocess(id);
        log.info("重新处理id={}文档成功", id);
        return Result.success();
    }
}
