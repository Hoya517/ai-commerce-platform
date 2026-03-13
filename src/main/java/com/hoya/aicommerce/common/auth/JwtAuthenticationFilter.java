package com.hoya.aicommerce.common.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String UNAUTHORIZED_BODY =
            "{\"success\":false,\"data\":null,\"message\":\"인증이 필요합니다\"}";

    private final JwtProvider jwtProvider;
    private final AuthContext authContext;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (token != null && jwtProvider.validateToken(token)) {
                authContext.set(jwtProvider.getMemberId(token));
            } else if (isProtectedPath(request)) {
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

    private boolean isProtectedPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        return path.startsWith("/cart") ||
                path.startsWith("/settlements") ||
                (path.equals("/orders") && "POST".equalsIgnoreCase(method)) ||
                (path.equals("/sellers") && "POST".equalsIgnoreCase(method)) ||
                (path.equals("/products") && "POST".equalsIgnoreCase(method)) ||
                (path.matches("/payments/\\d+/cancel") && "POST".equalsIgnoreCase(method));
    }

    private void sendUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(UNAUTHORIZED_BODY);
    }
}
