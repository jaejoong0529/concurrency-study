package com.concurrencystudy.service;

import com.concurrencystudy.domain.Coupon;
import com.concurrencystudy.domain.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * synchronized 사용
 * 단일 서버 환경에서는 동작하지만, 분산 환경에서는 작동하지 않음
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceV2 {

    private final CouponRepository couponRepository;

    public synchronized void issueCoupon(Long couponId) {
        issueCouponInternal(couponId);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
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
