package com.hoya.aicommerce.common.auth;

import org.springframework.stereotype.Component;

@Component
public class AuthContext {

    private static final ThreadLocal<Long> holder = new ThreadLocal<>();

    public void set(Long memberId) {
        holder.set(memberId);
    }

    public Long getMemberId() {
        return holder.get();
    }

    public void clear() {
        holder.remove();
    }
}
