package com.concurrencystudy.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer totalQuantity;

    @Column(nullable = false)
    private Integer issuedQuantity;

    @Version
    private Long version;

    @Builder
    private Coupon(String name, Integer totalQuantity, Integer issuedQuantity) {
        this.name = name;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = issuedQuantity == null ? 0 : issuedQuantity;
    }

    public static Coupon create(String name, Integer totalQuantity) {
        return Coupon.builder()
                .name(name)
                .totalQuantity(totalQuantity)
                .issuedQuantity(0)
                .build();
    }

    public boolean isAvailable() {
        return issuedQuantity < totalQuantity;
    }

    public void issue() {
        if (!isAvailable()) {
            throw new IllegalStateException("쿠폰이 모두 소진되었습니다.");
        }
        this.issuedQuantity++;
    }
}
