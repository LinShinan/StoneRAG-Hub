package com.stone.rag.controller;

import com.stone.rag.common.Result;
import com.stone.rag.dto.LoginDTO;
import com.stone.rag.dto.RegisterDTO;
import com.stone.rag.service.UserService;
import com.stone.rag.utils.UserContext;
import com.stone.rag.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@Tag(name="用户认证")
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户注册
     * @param dto
     * @return
     */
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody  RegisterDTO dto){
        UserVO userVO  = userService.register(dto);
        log.info("用户注册成功：{}", userVO);
        return Result.success(userVO);
    }

    /**
     * 用户登录
     * @param dto
     * @return
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<UserVO> login(@Valid @RequestBody LoginDTO dto){
        UserVO userVO = userService.login(dto);
        log.info("用户登录成功：{}", userVO);
        return Result.success(userVO);
    }

    /**
     * 获取当前用户信息
     * @return
     */
    @Operation(summary="当前用户信息")
    @GetMapping("/me")
    public Result<UserVO> me(){
        Long userId = UserContext.get();
        UserVO userVO = userService.getCurrentUser(userId);
        log.info("显示id={}的用户信息",userId);
        return Result.success(userVO);
    }



}
