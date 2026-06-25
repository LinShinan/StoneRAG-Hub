package com.stone.rag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stone.rag.entity.Document;
import com.stone.rag.vo.DocumentDetailVO;
import com.stone.rag.vo.PageVO;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService extends IService<Document> {

    /**
     * 上传文件
     * @param kbId
     * @param file
     * @return
     */
    Document upload(Long kbId, MultipartFile file);

    /**
     * 获取文件列表
     * @param kbId
     * @param page
     * @param size
     * @param status
     * @param fileType
     * @return
     */
    PageVO<Document> listDocuments(Long kbId, int page, int size, String status, String fileType);

    /**
     * 获取文件详情
     * @param id
     * @return
     */
    DocumentDetailVO getDetail(Long id);

    /**
     * 删除文件
     * @param id
     */
    void deleteDocument(Long id);

    /**
     * 重新处理失败文档
     * @param id
     */
    void reprocess(Long id);
}
