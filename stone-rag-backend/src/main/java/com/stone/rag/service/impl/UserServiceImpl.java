package com.stone.rag.service.impl;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stone.rag.common.ErrorCode;
import com.stone.rag.dto.LoginDTO;
import com.stone.rag.dto.RegisterDTO;
import com.stone.rag.entity.User;
import com.stone.rag.exception.BusinessException;
import com.stone.rag.mapper.UserMapper;
import com.stone.rag.service.UserService;
import com.stone.rag.utils.JwtUtils;
import com.stone.rag.utils.SecurityUtils;
import com.stone.rag.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final JwtUtils jwtUtils;

    /**
     * 用户注册
     * @param dto
     * @return
     */
    @Override
    public UserVO register(RegisterDTO dto) {
        //1. 校验用户邮箱唯一性
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getEmail, dto.getEmail());

        if(this.count(queryWrapper)>0){
            throw new BusinessException(ErrorCode.BAD_REQUEST,"该邮箱已注册");
        }
        //2. dto转entity
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPasswordHash(SecurityUtils.encode(dto.getPassword()));
        //3. 保存到数据库
        save(user);
        // 4. Entity->VO，生成 Token 返回
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .token(jwtUtils.generate(user.getId()))
                .build();
    }

    /**
     * 用户登录
     * @param dto
     * @return
     */
    @Override
    public UserVO login(LoginDTO dto) {
        //1. 根据邮箱查询用户是否存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getEmail, dto.getEmail());
        User user = getOne(queryWrapper);
        if(user==null){
            throw new BusinessException(ErrorCode.BAD_REQUEST,"邮箱或密码错误");
        }
        //2. 校验密码
        boolean matches = SecurityUtils.matches(dto.getPassword(), user.getPasswordHash());
        if(!matches){
            throw new BusinessException(ErrorCode.BAD_REQUEST,"邮箱或密码错误");
        }
        //3. 生成 Token，返回用户信息
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .token(jwtUtils.generate(user.getId()))
                .build();
    }

    /**
     * 获取当前用户信息
     * @param userId
     * @return
     */
    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = getById(userId);
        if(user==null){
            throw new BusinessException(ErrorCode.NOT_FOUND,"用户不存在");
        }
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .build();
    }
}
