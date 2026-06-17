package com.stone.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stone.rag.entity.Document;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocumentMapper extends BaseMapper<Document> {
}
