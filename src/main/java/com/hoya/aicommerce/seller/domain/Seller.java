package com.hoya.aicommerce.seller.domain;

import com.hoya.aicommerce.seller.exception.SellerException;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;
    private String businessName;
    private String settlementAccount;

    @Enumerated(EnumType.STRING)
    private SellerStatus status;

    private Seller(Long memberId, String businessName, String settlementAccount) {
        this.memberId = memberId;
        this.businessName = businessName;
        this.settlementAccount = settlementAccount;
        this.status = SellerStatus.PENDING;
    }

    public static Seller create(Long memberId, String businessName, String settlementAccount) {
        return new Seller(memberId, businessName, settlementAccount);
    }

    public void approve() {
        if (this.status != SellerStatus.PENDING) {
            throw new SellerException("판매자 승인은 PENDING 상태에서만 가능합니다");
        }
        this.status = SellerStatus.APPROVED;
    }

    public void suspend() {
        if (this.status == SellerStatus.SUSPENDED) {
            throw new SellerException("이미 정지된 판매자입니다");
        }
        this.status = SellerStatus.SUSPENDED;
    }

    public boolean isApproved() {
        return this.status == SellerStatus.APPROVED;
    }
}
