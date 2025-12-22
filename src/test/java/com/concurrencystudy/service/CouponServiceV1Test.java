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
 * 동시성 제어 상황 없는 상태
 * Race Condition 발생
 */
@SpringBootTest
class CouponServiceV1Test {

    @Autowired
    private CouponServiceV1 couponServiceV1;

    @Autowired
    private CouponRepository couponRepository;

    @BeforeEach
    void setUp() {
        couponRepository.deleteAll();
    }

    @Test
    @DisplayName("동시에 100명이 쿠폰 발급 시도 - Race Condition 발생")
    void raceConditionTest() throws InterruptedException {
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
                    couponServiceV1.issueCoupon(couponId);
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
        System.out.println("예상 발급 수량: 100");
        System.out.println("=========================================");

        assertThat(result.getIssuedQuantity()).isLessThan(100);
    }
}
