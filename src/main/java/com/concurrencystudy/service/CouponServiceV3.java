package com.concurrencystudy.service;

import com.concurrencystudy.domain.Coupon;
import com.concurrencystudy.domain.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 비관적 락 (Pessimistic Lock)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceV3 {

    private final CouponRepository couponRepository;

    @Transactional
    public void issueCoupon(Long couponId) {
        Coupon coupon = couponRepository.findByIdWithPessimisticLock(couponId)
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
