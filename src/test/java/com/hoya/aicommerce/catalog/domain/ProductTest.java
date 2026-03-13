package com.hoya.aicommerce.catalog.domain;

import com.hoya.aicommerce.catalog.exception.ProductException;
import com.hoya.aicommerce.common.domain.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    void 정상_생성된다() {
        Product product = Product.create("상품A", "설명", Money.of(1000L), 10, 1L);

        assertThat(product.getName()).isEqualTo("상품A");
        assertThat(product.getPrice()).isEqualTo(Money.of(1000L));
        assertThat(product.getStockQuantity()).isEqualTo(10);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(product.getSellerId()).isEqualTo(1L);
    }

    @Test
    void 가격이_0이면_예외가_발생한다() {
        assertThatThrownBy(() -> Product.create("상품A", "설명", Money.zero(), 10, 1L))
                .isInstanceOf(ProductException.class);
    }

    @Test
    void 재고가_정상_차감된다() {
        Product product = Product.create("상품A", "설명", Money.of(1000L), 10, 1L);
        product.decreaseStock(3);
        assertThat(product.getStockQuantity()).isEqualTo(7);
    }

    @Test
    void 재고가_부족하면_예외가_발생한다() {
        Product product = Product.create("상품A", "설명", Money.of(1000L), 5, 1L);
        assertThatThrownBy(() -> product.decreaseStock(10))
                .isInstanceOf(ProductException.class);
    }

    @Test
    void 재고가_0이되면_OUT_OF_STOCK으로_전환된다() {
        Product product = Product.create("상품A", "설명", Money.of(1000L), 5, 1L);
        product.decreaseStock(5);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.OUT_OF_STOCK);
    }

    @Test
    void ON_SALE_상태이면_isOnSale이_true다() {
        Product product = Product.create("상품A", "설명", Money.of(1000L), 10, 1L);
        assertThat(product.isOnSale()).isTrue();
    }

    @Test
    void OUT_OF_STOCK_상태이면_isOnSale이_false다() {
        Product product = Product.create("상품A", "설명", Money.of(1000L), 1, 1L);
        product.decreaseStock(1);
        assertThat(product.isOnSale()).isFalse();
    }
}
