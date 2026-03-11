package com.hoya.aicommerce.catalog.application;

import com.hoya.aicommerce.catalog.application.dto.CreateProductCommand;
import com.hoya.aicommerce.catalog.application.dto.ProductResult;
import com.hoya.aicommerce.catalog.domain.Product;
import com.hoya.aicommerce.catalog.domain.ProductRepository;
import com.hoya.aicommerce.catalog.domain.ProductStatus;
import com.hoya.aicommerce.catalog.exception.ProductException;
import com.hoya.aicommerce.common.domain.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void 상품이_생성된다() {
        Product product = Product.create("상품A", "설명", Money.of(1000L), 10);
        given(productRepository.save(any())).willReturn(product);

        CreateProductCommand command = new CreateProductCommand("상품A", "설명", BigDecimal.valueOf(1000), 10);
        ProductResult result = productService.createProduct(command);

        assertThat(result.name()).isEqualTo("상품A");
        assertThat(result.price()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(result.stockQuantity()).isEqualTo(10);
        assertThat(result.status()).isEqualTo(ProductStatus.ON_SALE);
        verify(productRepository).save(any());
    }

    @Test
    void 상품을_조회한다() {
        Product product = Product.create("상품A", "설명", Money.of(1000L), 10);
        given(productRepository.findById(1L)).willReturn(Optional.of(product));

        ProductResult result = productService.getProduct(1L);

        assertThat(result.name()).isEqualTo("상품A");
        assertThat(result.status()).isEqualTo(ProductStatus.ON_SALE);
    }

    @Test
    void 존재하지_않는_상품_조회시_예외가_발생한다() {
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(99L))
                .isInstanceOf(ProductException.class);
    }

    @Test
    void 상품_상태가_변경된다() {
        Product product = Product.create("상품A", "설명", Money.of(1000L), 10);
        given(productRepository.findById(1L)).willReturn(Optional.of(product));

        productService.changeProductStatus(1L, ProductStatus.HIDDEN);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.HIDDEN);
    }

    @Test
    void 존재하지_않는_상품_상태_변경시_예외가_발생한다() {
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.changeProductStatus(99L, ProductStatus.HIDDEN))
                .isInstanceOf(ProductException.class);
    }
}
