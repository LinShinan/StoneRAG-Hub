package com.stone.rag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stone.rag.dto.KnowledgeBaseCreateDTO;
import com.stone.rag.dto.KnowledgeBaseUpdateDTO;
import com.stone.rag.entity.KnowledgeBase;
import com.stone.rag.vo.KnowledgeBaseStatsVO;
import com.stone.rag.vo.PageVO;
import jakarta.validation.Valid;

public interface KnowledgeBaseService extends IService<KnowledgeBase> {

    /**
     * 创建知识库
     * @param dto
     * @return
     */
    KnowledgeBase createBase(@Valid KnowledgeBaseCreateDTO dto);

    /**
     * 获取知识库列表
     * @param page
     * @param size
     * @param keyword
     * @return
     */
    PageVO<KnowledgeBase> listBases(int page, int size, String keyword);

    /**
     * 根据id获取知识库
     * @param id
     * @return
     */
    KnowledgeBase getBaseById(Long id);

    /**
     * 更新知识库
     * @param id
     * @param knowledgeBaseUpdateDTO
     * @return
     */
    KnowledgeBase updateBase(Long id, @Valid KnowledgeBaseUpdateDTO knowledgeBaseUpdateDTO);

    /**
     * 删除知识库
     * @param id
     */
    void deletBaseById(Long id);

    /**
     * 获取知识库统计信息
     * @param id
     * @return
     */
    KnowledgeBaseStatsVO statistics(Long id);
}
