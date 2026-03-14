package com.hoya.aicommerce.catalog.application;

import com.hoya.aicommerce.catalog.domain.Product;
import com.hoya.aicommerce.catalog.domain.ProductRepository;
import com.hoya.aicommerce.catalog.exception.ProductException;
import com.hoya.aicommerce.catalog.infrastructure.RedisStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Redis 선차감 기반 재고 관리 서비스.
 *
 * 흐름:
 *   1. Redis 원자적 차감 → 재고 없으면 즉시 거절 (DB 미접근)
 *   2. 차감 성공 시 DB 차감 (비관적 락)
 *   3. DB 실패 시 Redis 복구 (보상 처리)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
public class StockService {

    private final RedisStockRepository redisStockRepository;
    private final ProductRepository productRepository;

    /**
     * 재고를 Redis → DB 순서로 차감한다.
     * Redis에 재고가 없으면 빠른 거절.
     */
    @Transactional
    public void decrease(Long productId, int quantity) {
        long remaining = redisStockRepository.decrease(productId, quantity);

        if (remaining == -2) {
            // Redis 미초기화 → DB 기준으로 처리 (fallback)
            log.warn("Redis stock not initialized for product {}. Falling back to DB.", productId);
            decreaseFromDb(productId, quantity);
            return;
        }

        if (remaining < 0) {
            throw new ProductException("Insufficient stock");
        }

        try {
            decreaseFromDb(productId, quantity);
        } catch (Exception e) {
            // DB 실패 시 Redis 재고 복구 (보상 트랜잭션)
            redisStockRepository.increase(productId, quantity);
            throw e;
        }
    }

    /**
     * 재고를 복구한다 (주문 취소/결제 취소 시).
     */
    @Transactional
    public void increase(Long productId, int quantity) {
        redisStockRepository.increase(productId, quantity);
        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ProductException("Product not found"));
        product.increaseStock(quantity);
    }

    /**
     * 상품 등록/수정 시 Redis 재고를 동기화한다.
     */
    public void sync(Long productId, int quantity) {
        redisStockRepository.sync(productId, quantity);
    }

    private void decreaseFromDb(Long productId, int quantity) {
        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ProductException("Product not found"));
        product.decreaseStock(quantity);
    }
}
