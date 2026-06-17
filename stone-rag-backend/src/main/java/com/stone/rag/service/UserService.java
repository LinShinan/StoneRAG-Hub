package com.stone.rag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stone.rag.dto.LoginDTO;
import com.stone.rag.dto.RegisterDTO;
import com.stone.rag.entity.User;
import com.stone.rag.vo.UserVO;
import jakarta.validation.Valid;

public interface UserService extends IService<User> {

    /**
     *  用户注册
     * @param dto
     * @return
     */
    UserVO register(RegisterDTO dto);

    /**
     * 用户登录
     * @param dto
     * @return
     */
    UserVO login(@Valid LoginDTO dto);

    /**
     * 获取当前用户信息
     * @param userId
     * @return
     */
    UserVO getCurrentUser(Long userId);
}
