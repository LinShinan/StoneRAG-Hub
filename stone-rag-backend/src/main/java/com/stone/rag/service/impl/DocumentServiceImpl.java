package com.stone.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stone.rag.common.ErrorCode;
import com.stone.rag.constant.DocStatus;
import com.stone.rag.dto.AiParseRequest;
import com.stone.rag.entity.Document;
import com.stone.rag.entity.DocumentChunk;
import com.stone.rag.exception.BusinessException;
import com.stone.rag.mapper.DocumentMapper;
import com.stone.rag.service.AiClient;
import com.stone.rag.service.DocumentChunkService;
import com.stone.rag.service.DocumentService;
import com.stone.rag.utils.FileUtils;
import com.stone.rag.utils.UserContext;
import com.stone.rag.vo.AiParseResponse;
import com.stone.rag.vo.DocumentDetailVO;
import com.stone.rag.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, Document> implements DocumentService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final AiClient aiClient;

    private final DocumentChunkService documentChunkService;

    /**
     * 上传文件
     * @param kbId
     * @param file
     * @return
     */
    @Override
    public Document upload(Long kbId, MultipartFile file) {
        //1.保存文件到磁盘
        String savedPath = FileUtils.save(file, uploadDir);
        //2. 写入文档记录
        Document doc = new Document();
        doc.setKbId(kbId);
        doc.setUserId(UserContext.get());
        doc.setTitle(file.getOriginalFilename());
        doc.setFileType(FileUtils.getExtension(file.getOriginalFilename()));
        doc.setFileSize(file.getSize());
        doc.setFilePath(savedPath);
        doc.setStatus(DocStatus.PROCESSING);
        this.save(doc);

        //3. 开新线程解析文件
        CompletableFuture.runAsync(()->processDocument(doc));
        return doc;
    }

    /**
     * 列出文件
     * @param kbId
     * @param page
     * @param size
     * @param status
     * @param fileType
     * @return
     */
    @Override
    public PageVO<Document> listDocuments(Long kbId, int page, int size, String status, String fileType) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getKbId,kbId)
                .eq(StringUtils.hasText(status), Document::getStatus, status)
                .eq(StringUtils.hasText(fileType), Document::getFileType, fileType)
                .orderByDesc(Document::getCreatedAt);

        Page<Document> result = this.page(new Page<>(page, size), wrapper);

        PageVO<Document> vo = new PageVO<>();
        vo.setItems(result.getRecords());
        vo.setTotal(result.getTotal());
        vo.setPage(page);
        vo.setSize(size);
        return vo;
    }

    /**
     * 获取文件详情
     * @param docId
     * @return
     */
    @Override
    public DocumentDetailVO getDetail(Long docId) {
        Document doc = this.getById(docId);
        if (doc == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文档不存在");
        }

        // Document 转VO
        DocumentDetailVO vo = new DocumentDetailVO();
        BeanUtils.copyProperties(doc, vo);

        // String -> List<String>
        if (StringUtils.hasText(doc.getTagsJson())) {
            try {
                vo.setTagsJson(new ObjectMapper().readValue(
                        doc.getTagsJson(), new TypeReference<List<String>>() {}));
            } catch (Exception e) {
                vo.setTagsJson(List.of());
            }
        }

        // 查分段预览，最多 20 条
        LambdaQueryWrapper<DocumentChunk> qw = new LambdaQueryWrapper<>();
        qw.eq(DocumentChunk::getDocumentId, docId)
                .orderByAsc(DocumentChunk::getChunkIndex)
                .last("LIMIT 20");

        List<DocumentChunk> chunks = documentChunkService.list(qw);
        if (!chunks.isEmpty()) {
            vo.setChunksPreview(chunks.stream().map(c -> {
                DocumentDetailVO.ChunkPreview cp = new DocumentDetailVO.ChunkPreview();
                cp.setIndex(c.getChunkIndex());
                cp.setContentText(c.getContentText());
                cp.setCharCount(c.getCharCount());
                return cp;
            }).toList());
        }

        return vo;
    }


    /**
     * 删除文件
     * @param docId
     */
    @Transactional
    @Override
    public void deleteDocument(Long docId) {
        // 1. 查文档
        Document doc = this.getById(docId);
        if (doc == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文档不存在");
        }

        // 2. 删 MySQL document_chunks
        LambdaQueryWrapper<DocumentChunk> qw = new LambdaQueryWrapper<>();
        qw.eq(DocumentChunk::getDocumentId, docId);
        documentChunkService.remove(qw);

        // 3. 删 MySQL documents 记录
        this.removeById(docId);

        // 4. TODO: 调 Python POST /rag/delete 删 ChromaDB 向量

        // 5. 最后删磁盘文件（数据库成功后再删，万一出问题文件还在）
        if (StringUtils.hasText(doc.getFilePath())) {
            File file = new File(doc.getFilePath());
            if (file.exists()) {
                file.delete();
            }
        }
    }

    /**
     * 重新处理失败文档
     * @param docId
     */
    @Transactional
    @Override
    public void reprocess(Long docId) {
        Document doc = this.getById(docId);
        if (doc == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文档不存在");
        }
        if (!DocStatus.FAILED.equals(doc.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只有失败状态的文档才能重试");
        }

        // 清掉旧分段
        LambdaQueryWrapper<DocumentChunk> qw = new LambdaQueryWrapper<>();
        qw.eq(DocumentChunk::getDocumentId, docId);
        documentChunkService.remove(qw);

        // 重置状态
        doc.setStatus(DocStatus.PROCESSING);
        doc.setErrorMsg(null);
        this.updateById(doc);

        // 重新异步解析
        CompletableFuture.runAsync(() -> processDocument(doc));
    }

    /**
     * 处理文件
     * @param doc
     */
    public void processDocument(Document doc){
        try {
            // 调 Python 解析
            AiParseRequest request = new AiParseRequest();
            request.setDocId(doc.getId().intValue());
            request.setFilePath(doc.getFilePath());
            request.setFileType(doc.getFileType());
            AiParseResponse result = aiClient.parse(request);

            // 保存分段
            // TODO: 存 document_chunks 表
            List<DocumentChunk> docChunks = result.getChunks()
                    .stream()
                    .map(item -> toChunk(item, doc))
                    .collect(Collectors.toList());
            documentChunkService.saveBatch(docChunks);
            // 更新状态为就绪可用
            doc.setStatus(DocStatus.READY);
            doc.setChunkCount(result.getChunks().size());
            this.updateById(doc);

        } catch (Exception e) {
            doc.setStatus(DocStatus.FAILED);
            doc.setErrorMsg(e.getMessage());
            this.updateById(doc);
        }
    }

    private DocumentChunk toChunk(AiParseResponse.ChunkItem item, Document doc) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setDocumentId(doc.getId());
        chunk.setKbId(doc.getKbId());
        chunk.setChunkIndex(item.getIndex());
        chunk.setChunkHash(item.getHash());
        chunk.setContentText(item.getText());
        chunk.setCharCount(item.getCharCount());
        return chunk;
    }
}
