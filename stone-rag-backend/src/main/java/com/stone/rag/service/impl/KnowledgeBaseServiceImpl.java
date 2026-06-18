package com.stone.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stone.rag.common.ErrorCode;
import com.stone.rag.dto.KnowledgeBaseCreateDTO;
import com.stone.rag.dto.KnowledgeBaseUpdateDTO;
import com.stone.rag.entity.KnowledgeBase;
import com.stone.rag.exception.BusinessException;
import com.stone.rag.mapper.KnowledgeBaseMapper;
import com.stone.rag.service.KnowledgeBaseService;
import com.stone.rag.utils.UserContext;
import com.stone.rag.vo.KnowledgeBaseStatsVO;
import com.stone.rag.vo.PageVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements KnowledgeBaseService {

    /**
     * 创建知识库
     * @param dto
     * @return
     */
    @Override
    public KnowledgeBase createBase(KnowledgeBaseCreateDTO dto) {
        //1. 拿当前用户id
        Long userId = UserContext.get();
        //2. dto-> entity
        KnowledgeBase kb = new KnowledgeBase();
        BeanUtils.copyProperties(dto, kb);
        kb.setUserId(userId);
        kb.setDocCount(0);
        kb.setTotalChunks(0);
        //3. 保存数据库
        save(kb);
        //4. 返回
        return kb;
    }

    /**
     * 获取知识库列表
     * @param page
     * @param size
     * @param keyword
     * @return
     */
    @Override
    public PageVO<KnowledgeBase> listBases(int page, int size, String keyword) {
        //1. 构建查询条件，获取自己的，按更新时间倒序，可以名称搜索
        LambdaQueryWrapper<KnowledgeBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeBase::getUserId, UserContext.get())
                .like(StringUtils.hasText(keyword),KnowledgeBase::getName, keyword)
                .orderByDesc(KnowledgeBase::getUpdatedAt);

        //2. 分页查询
        IPage<KnowledgeBase> result = page(new Page<>(page, size), queryWrapper);

        //3. 封装成VO返回
        PageVO<KnowledgeBase> pageVO = new PageVO<>();
        pageVO.setItems(result.getRecords());
        pageVO.setTotal(result.getTotal());
        pageVO.setPage(page);
        pageVO.setSize(size);
        return pageVO;
    }


    /**
     * 获取知识库详情
     * @param id
     * @return
     */
    @Override
    public KnowledgeBase getBaseById(Long id) {
        Long userId = UserContext.get();
        KnowledgeBase kb = getById(id);
        checkPermission(kb,"访问");
        return kb;
    }

    /**
     * 更新知识库
     * @param id
     * @param dto
     * @return
     */
    @Override
    public KnowledgeBase updateBase(Long id, KnowledgeBaseUpdateDTO dto) {
        //1. 获取知识库并校验权限
        KnowledgeBase kb = getById(id);
        Long userId = UserContext.get();
        checkPermission(kb,"修改");
        //2. 更新知识库
        if(StringUtils.hasText(dto.getName())){
            kb.setName(dto.getName());
        }
        if(dto.getDescription()!=null){ //可以改成空字符串的，所以不用hasText校验
            kb.setDescription(dto.getDescription());
        }
        if(dto.getIcon()!=null){
            kb.setIcon(dto.getIcon());
        }
        updateById(kb);
        //3. 返回
        return kb;
    }

    /**
     * 删除知识库
     * @param id
     */
    @Override
    public void deletBaseById(Long id) {
        //TODO 级联删除，目前一些地方还没写，暂时先只删除知识库表的
        KnowledgeBase kb= getById(id);
        checkPermission(kb,"删除");
        removeById(id);
    }

    /**
     * 获取知识库统计信息
     * @param id
     * @return
     */
    @Override
    public KnowledgeBaseStatsVO statistics(Long id) {
        KnowledgeBase kb = getById(id);
        checkPermission(kb,"访问");
        KnowledgeBaseStatsVO vo = new KnowledgeBaseStatsVO();
        vo.setDocCount(kb.getDocCount());
        vo.setChunkCount(kb.getTotalChunks());
        vo.setTotalChars(0L);// TODO: SUM from document_chunks
        vo.setFileTypeDist(Map.of());// TODO: GROUP BY from documents
        vo.setRecentDocs(List.of());// TODO: LIMIT 5 from documents
        return vo;
    }


    /**
     * 校验权限
     * @param kb
     * @param action
     */
    private void checkPermission(KnowledgeBase kb,String action){
        if(kb==null){
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在");
        }
        Long userId = UserContext.get();
        if(!kb.getUserId().equals(userId)){
            throw new BusinessException(ErrorCode.UNAUTHORIZED,"无权"+action+"该知识库");
        }
    }
}
