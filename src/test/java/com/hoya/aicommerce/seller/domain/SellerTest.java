package com.hoya.aicommerce.seller.domain;

import com.hoya.aicommerce.seller.exception.SellerException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SellerTest {

    @Test
    void 정상_생성된다() {
        Seller seller = Seller.create(1L, "홍길동상회", "110-1234-567890");

        assertThat(seller.getMemberId()).isEqualTo(1L);
        assertThat(seller.getBusinessName()).isEqualTo("홍길동상회");
        assertThat(seller.getStatus()).isEqualTo(SellerStatus.PENDING);
        assertThat(seller.isApproved()).isFalse();
    }

    @Test
    void PENDING_상태에서_승인된다() {
        Seller seller = Seller.create(1L, "홍길동상회", "110-1234-567890");

        seller.approve();

        assertThat(seller.getStatus()).isEqualTo(SellerStatus.APPROVED);
        assertThat(seller.isApproved()).isTrue();
    }

    @Test
    void PENDING이_아닌_상태에서_승인하면_예외가_발생한다() {
        Seller seller = Seller.create(1L, "홍길동상회", "110-1234-567890");
        seller.approve();

        assertThatThrownBy(seller::approve)
                .isInstanceOf(SellerException.class);
    }

    @Test
    void 정지_처리된다() {
        Seller seller = Seller.create(1L, "홍길동상회", "110-1234-567890");
        seller.approve();

        seller.suspend();

        assertThat(seller.getStatus()).isEqualTo(SellerStatus.SUSPENDED);
    }

    @Test
    void 이미_정지된_판매자를_정지하면_예외가_발생한다() {
        Seller seller = Seller.create(1L, "홍길동상회", "110-1234-567890");
        seller.approve();
        seller.suspend();

        assertThatThrownBy(seller::suspend)
                .isInstanceOf(SellerException.class);
    }
}
