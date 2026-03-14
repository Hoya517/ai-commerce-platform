package com.hoya.aicommerce.common.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 인증이 필요한 컨트롤러 메서드 또는 클래스에 붙이는 어노테이션.
 * AuthHandlerInterceptor가 JWT 인증 여부를 검사하고, 미인증 요청은 401로 거부한다.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresAuth {
}
