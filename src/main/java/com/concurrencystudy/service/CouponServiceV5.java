package com.concurrencystudy.service;

import com.concurrencystudy.domain.Coupon;
import com.concurrencystudy.domain.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * Redis 분산 락 (Distributed Lock) - Redisson 직접 사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceV5 {

    private final CouponRepository couponRepository;
    private final RedissonClient redissonClient;
    private static final String LOCK_PREFIX = "LOCK:";

    public void issueCoupon(Long couponId) {
        String lockKey = LOCK_PREFIX + couponId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean available = lock.tryLock(5L, 3L, TimeUnit.SECONDS);
            if (!available) {
                log.warn("락 획득 실패. key: {}", lockKey);
                throw new IllegalStateException("다른 사용자가 처리 중입니다. 잠시 후 다시 시도해주세요.");
            }

            log.debug("락 획득 성공. key: {}", lockKey);
            
            issueCouponInternal(couponId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("락 획득 중 인터럽트 발생", e);
        } finally {
            try {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.debug("락 해제 성공. key: {}", lockKey);
                }
            } catch (IllegalMonitorStateException e) {
                log.warn("락 해제 실패. 이미 해제되었거나 다른 스레드가 소유하고 있습니다. key: {}", lockKey);
            }
        }
    }

    @Transactional
    protected void issueCouponInternal(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));

        if (coupon.isAvailable()) {
            coupon.issue();
            couponRepository.save(coupon);
            log.info("쿠폰 발급 성공! 남은 수량: {}", coupon.getTotalQuantity() - coupon.getIssuedQuantity());
        } else {
            log.warn("쿠폰이 모두 소진되었습니다.");
            throw new IllegalStateException("쿠폰이 모두 소진되었습니다.");
        }
    }
}
