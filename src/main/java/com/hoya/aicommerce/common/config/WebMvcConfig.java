package com.hoya.aicommerce.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 설정.
 * 인증은 JwtAuthenticationFilter (@RequiresAuth 어노테이션 기반)에서 처리한다.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
}
