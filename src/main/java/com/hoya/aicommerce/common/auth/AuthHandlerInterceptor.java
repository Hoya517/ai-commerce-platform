package com.hoya.aicommerce.common.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@RequiredArgsConstructor
public class AuthHandlerInterceptor implements HandlerInterceptor {

    private static final String UNAUTHORIZED_BODY =
            "{\"success\":false,\"data\":null,\"message\":\"인증이 필요합니다\"}";

    private final AuthContext authContext;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        boolean requiresAuth = handlerMethod.hasMethodAnnotation(RequiresAuth.class)
                || handlerMethod.getBeanType().isAnnotationPresent(RequiresAuth.class);

        if (requiresAuth && authContext.getMemberId() == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(UNAUTHORIZED_BODY);
            return false;
        }

        return true;
    }
}
