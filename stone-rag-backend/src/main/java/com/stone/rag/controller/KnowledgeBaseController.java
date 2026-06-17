package com.stone.rag.controller;

import com.stone.rag.common.Result;
import com.stone.rag.dto.KnowledgeBaseCreateDTO;
import com.stone.rag.vo.KnowledgeBaseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name="知识库")
@Slf4j
@RestController
@RequestMapping("/api/knowledge-bases")
public class KnowledgeBaseController {

    @Operation(summary="创建知识库")
    @PostMapping
    public Result<KnowledgeBaseVO> createBase(KnowledgeBaseCreateDTO dto){

        return Result.success();
    }
}
