package com.stone.rag.controller;

import com.stone.rag.common.Result;
import com.stone.rag.dto.KnowledgeBaseCreateDTO;
import com.stone.rag.dto.KnowledgeBaseUpdateDTO;
import com.stone.rag.entity.Document;
import com.stone.rag.entity.KnowledgeBase;
import com.stone.rag.mapper.DocumentMapper;
import com.stone.rag.service.DocumentService;
import com.stone.rag.service.KnowledgeBaseService;
import com.stone.rag.vo.KnowledgeBaseStatsVO;
import com.stone.rag.vo.PageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@Tag(name="知识库")
@Slf4j
@RestController
@RequestMapping("/api/knowledge-bases")
public class KnowledgeBaseController {


    private final KnowledgeBaseService knowledgeBaseService;


    private final DocumentService documentService;


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

    /**
     * 获取知识库统计信息
     * @param id
     * @return
     */
    @Operation(summary="知识库统计")
    @GetMapping("/{id}/stats")
    public Result<KnowledgeBaseStatsVO> statistics(@PathVariable Long id){
        KnowledgeBaseStatsVO vo = knowledgeBaseService.statistics(id);
        log.info("获取知识库id={}统计信息成功：{}", id, vo);
        return Result.success(vo);
    }


    /**
     * 上传文件并加载
     * @param kbId
     * @param file
     * @return
     */
    @Operation(summary="上传文件", description = "支持docx,pdf,txt")
    @PostMapping(value = "/{kbId}/documents/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Document> uploadDocuments(@PathVariable Long kbId, @RequestParam("file") MultipartFile file){
        Document document = documentService.upload(kbId,file);
        log.info("上传文件成功：{}", document);
        return Result.success(document);
    }

    /**
     * 获取文件列表
     * @param kbId
     * @param page
     * @param size
     * @param status
     * @param fileType
     * @return
     */
    @Operation(summary="获取文件列表")
    @GetMapping("/{kbId}/documents")
    public Result<PageVO<Document>> listDocuments(
            @PathVariable("kbId") Long kbId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String fileType
    ){
        PageVO<Document> vo = documentService.listDocuments(kbId,page,size,status,fileType);
        return Result.success(vo);
    }




}
