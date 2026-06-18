package com.stone.rag.controller;

import com.stone.rag.common.Result;
import com.stone.rag.dto.KnowledgeBaseCreateDTO;
import com.stone.rag.dto.KnowledgeBaseUpdateDTO;
import com.stone.rag.entity.KnowledgeBase;
import com.stone.rag.service.KnowledgeBaseService;
import com.stone.rag.vo.KnowledgeBaseStatsVO;
import com.stone.rag.vo.PageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name="知识库")
@Slf4j
@RestController
@RequestMapping("/api/knowledge-bases")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    /**
     * 创建知识库
     * @param dto
     * @return
     */
    @Operation(summary="创建知识库")
    @PostMapping
    public Result<KnowledgeBase> createBase(@Valid  @RequestBody KnowledgeBaseCreateDTO dto){
        KnowledgeBase knowledgeBase = knowledgeBaseService.createBase(dto);
        log.info("创建知识库成功：{}", knowledgeBase);
        return Result.success(knowledgeBase);
    }

    /**
     * 获取知识库列表
     * @param page
     * @param size
     * @param keyword
     * @return
     */
    @GetMapping
    public Result<PageVO<KnowledgeBase>> listBases(
            @RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="10") int size,
            @RequestParam(required = false) String keyword
    ){
        PageVO<KnowledgeBase> vo = knowledgeBaseService.listBases(page,size,keyword);
        log.info("分页查询知识库成功");
        return Result.success(vo);
    }

    /**
     * 获取知识库详情
     * @param id
     * @return
     */
    @Operation(summary = "知识库详情")
    @GetMapping("/{id}")
    public Result<KnowledgeBase> getBase(@PathVariable Long id) {
        KnowledgeBase knowledgeBase = knowledgeBaseService.getBaseById(id);
        log.info("获取知识库详情成功：{}", knowledgeBase);
        return Result.success(knowledgeBase);
    }

    /**
     * 更新知识库
     * @param id
     * @param knowledgeBaseUpdateDTO
     * @return
     */
    @Operation(summary="更新知识库")
    @PutMapping("/{id}")
    public Result<KnowledgeBase> updateBase(
            @PathVariable Long id,
            @Valid @RequestBody KnowledgeBaseUpdateDTO knowledgeBaseUpdateDTO
    ){
        KnowledgeBase base = knowledgeBaseService.updateBase(id, knowledgeBaseUpdateDTO);
        log.info("更新id={}知识库成功", id);
        return Result.success(base);
    }

    /**
     * 删除知识库
     * @param id
     * @return
     */
    @Operation(summary="删除知识库")
    @DeleteMapping("/{id}")
    public Result<Void> deleteBase(@PathVariable Long id){
        knowledgeBaseService.deletBaseById(id);
        log.info("删除id={}知识库成功", id);
        return Result.success();
    }

    @Operation(summary="知识库统计")
    @GetMapping("/{id}/stats")
    public Result<KnowledgeBaseStatsVO> statistics(@PathVariable Long id){
        KnowledgeBaseStatsVO vo = knowledgeBaseService.statistics(id);
        log.info("获取知识库id={}统计信息成功：{}", id, vo);
        return Result.success(vo);
    }


}
