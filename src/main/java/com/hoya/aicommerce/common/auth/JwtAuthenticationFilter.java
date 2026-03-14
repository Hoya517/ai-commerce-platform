package com.hoya.aicommerce.common.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;

/**
 * JWT 파싱 및 인증이 필요한 엔드포인트(@RequiresAuth) 보호 필터.
 * isProtectedPath() 하드코딩 없이 @RequiresAuth 어노테이션으로 선언적으로 관리.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String UNAUTHORIZED_BODY =
            "{\"success\":false,\"data\":null,\"message\":\"인증이 필요합니다\"}";

    private final JwtProvider jwtProvider;
    private final AuthContext authContext;
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (token != null && jwtProvider.validateToken(token)) {
                authContext.set(jwtProvider.getMemberId(token));
            } else if (requiresAuth(request)) {
                sendUnauthorized(response);
                return;
            }
            filterChain.doFilter(request, response);
        } finally {
            authContext.clear();
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private boolean requiresAuth(HttpServletRequest request) {
        try {
            var chain = requestMappingHandlerMapping.getHandler(request);
            if (chain == null) return false;
            Object handler = chain.getHandler();
            if (!(handler instanceof HandlerMethod handlerMethod)) return false;
            return handlerMethod.hasMethodAnnotation(RequiresAuth.class)
                    || handlerMethod.getBeanType().isAnnotationPresent(RequiresAuth.class);
        } catch (Exception e) {
            return false;
        }
    }

    private void sendUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(UNAUTHORIZED_BODY);
    }
}
