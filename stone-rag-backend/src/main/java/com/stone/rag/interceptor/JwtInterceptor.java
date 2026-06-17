package com.stone.rag.interceptor;

import com.stone.rag.utils.JwtUtils;
import com.stone.rag.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 1. 拿 Token
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":40002,\"message\":\"未登录\"}");
            return false;
        }

        // 2. 验签
        String token = header.substring(7);
        if (!jwtUtils.validate(token)) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":40002,\"message\":\"Token无效或已过期\"}");
            return false;
        }

        // 3. 存 ThreadLocal
        Long userId = jwtUtils.getUserId(token);
        UserContext.set(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) throws Exception {
        UserContext.remove(); // 防止内存泄漏
    }
}
