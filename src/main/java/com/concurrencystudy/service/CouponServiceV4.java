package com.concurrencystudy.service;

import com.concurrencystudy.domain.Coupon;
import com.concurrencystudy.domain.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *낙관적 락 (Optimistic Lock)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceV4 {

    private final CouponRepository couponRepository;
    private static final int MAX_RETRY_COUNT = 20;

    public void issueCoupon(Long couponId) {
        int retryCount = 0;
        while (retryCount < MAX_RETRY_COUNT) {
            try {
                issueCouponInternal(couponId);
                return;
            } catch (OptimisticLockingFailureException e) {
                retryCount++;
                log.warn("낙관적 락 충돌 발생. 재시도 {}/{}", retryCount, MAX_RETRY_COUNT);
                if (retryCount >= MAX_RETRY_COUNT) {
                    throw new IllegalStateException("쿠폰 발급에 실패했습니다. 잠시 후 다시 시도해주세요.", e);
                }
                try {
                    long sleepTime = (long) (50 * Math.pow(2, retryCount - 1));
                    Thread.sleep(Math.min(sleepTime, 500));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("쿠폰 발급 중단됨", ie);
                }
            } catch (IllegalStateException e) {
                if (e.getMessage() != null && e.getMessage().contains("소진")) {
                    throw e;
                }
                retryCount++;
                if (retryCount >= MAX_RETRY_COUNT) {
                    throw e;
                }
            }
        }
    }

    @Transactional
    public void issueCouponInternal(Long couponId) {
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
