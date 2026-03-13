package com.hoya.aicommerce.wallet.domain;

import com.hoya.aicommerce.common.domain.Money;
import com.hoya.aicommerce.wallet.exception.WalletException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "balance"))
    private Money balance;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private Wallet(Long memberId) {
        this.memberId = memberId;
        this.balance = Money.zero();
    }

    public static Wallet create(Long memberId) {
        return new Wallet(memberId);
    }

    public void charge(Money amount) {
        this.balance = this.balance.add(amount);
    }

    public void deduct(Money amount) {
        if (this.balance.getValue().compareTo(amount.getValue()) < 0) {
            throw new WalletException("잔액이 부족합니다");
        }
        this.balance = this.balance.subtract(amount);
    }
}
