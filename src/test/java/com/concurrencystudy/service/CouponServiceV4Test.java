package com.concurrencystudy.service;

import com.concurrencystudy.domain.Coupon;
import com.concurrencystudy.domain.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 낙관적 락 테스트
 */
@SpringBootTest
class CouponServiceV4Test {

    @Autowired
    private CouponServiceV4 couponServiceV4;

    @Autowired
    private CouponRepository couponRepository;

    @BeforeEach
    void setUp() {
        couponRepository.deleteAll();
    }

    @Test
    @DisplayName("낙관적 락으로 동시성 제어 - 재시도 로직 포함")
    void optimisticLockTest() throws InterruptedException {
        // given
        Coupon coupon = Coupon.create("선착순 쿠폰", 100);
        Coupon saved = couponRepository.save(coupon);
        Long couponId = saved.getId();

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    couponServiceV4.issueCoupon(couponId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        Coupon result = couponRepository.findById(couponId).orElseThrow();
        System.out.println("=========================================");
        System.out.println("총 요청 수: " + threadCount);
        System.out.println("성공한 요청 수: " + successCount.get());
        System.out.println("실패한 요청 수: " + failCount.get());
        System.out.println("실제 발급된 쿠폰 수: " + result.getIssuedQuantity());
        System.out.println("=========================================");

        assertThat(result.getIssuedQuantity()).isEqualTo(100);
    }
}
