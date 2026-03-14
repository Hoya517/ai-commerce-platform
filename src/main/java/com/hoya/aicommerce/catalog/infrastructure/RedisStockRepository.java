package com.hoya.aicommerce.catalog.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Redis 기반 재고 선차감 저장소.
 * Lua 스크립트로 원자적 DECR + 음수 체크 처리.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
public class RedisStockRepository {

    private static final String STOCK_KEY_PREFIX = "product:stock:";

    // Lua: 재고가 충분하면 차감하고 남은 재고 반환, 부족하면 -1 반환
    private static final DefaultRedisScript<Long> DECR_SCRIPT = new DefaultRedisScript<>("""
            local current = tonumber(redis.call('GET', KEYS[1]))
            if current == nil then
                return -2
            end
            if current < tonumber(ARGV[1]) then
                return -1
            end
            return redis.call('DECRBY', KEYS[1], ARGV[1])
            """, Long.class);

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Redis에 재고를 초기화(동기화)한다.
     */
    public void sync(Long productId, int quantity) {
        redisTemplate.opsForValue().set(stockKey(productId), String.valueOf(quantity));
    }

    /**
     * 재고를 원자적으로 차감한다.
     *
     * @return 차감 후 남은 재고 (재고 부족 시 -1, 미초기화 시 -2)
     */
    public long decrease(Long productId, int quantity) {
        Long result = redisTemplate.execute(
                DECR_SCRIPT,
                List.of(stockKey(productId)),
                String.valueOf(quantity)
        );
        return result != null ? result : -1L;
    }

    /**
     * 재고를 복구한다 (취소/환불 시).
     */
    public void increase(Long productId, int quantity) {
        redisTemplate.opsForValue().increment(stockKey(productId), quantity);
    }

    /**
     * 현재 Redis 재고를 조회한다.
     *
     * @return Redis에 값이 없으면 null
     */
    public Integer get(Long productId) {
        String value = redisTemplate.opsForValue().get(stockKey(productId));
        return value != null ? Integer.parseInt(value) : null;
    }

    private String stockKey(Long productId) {
        return STOCK_KEY_PREFIX + productId;
    }
}
