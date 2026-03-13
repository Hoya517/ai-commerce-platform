package com.hoya.aicommerce.catalog.application;

import com.hoya.aicommerce.catalog.application.dto.CreateProductCommand;
import com.hoya.aicommerce.catalog.application.dto.ProductResult;
import com.hoya.aicommerce.catalog.domain.Product;
import com.hoya.aicommerce.catalog.domain.ProductRepository;
import com.hoya.aicommerce.catalog.domain.ProductStatus;
import com.hoya.aicommerce.catalog.exception.ProductException;
import com.hoya.aicommerce.common.domain.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResult createProduct(CreateProductCommand command) {
        Product product = Product.create(
                command.name(),
                command.description(),
                Money.of(command.price()),
                command.stockQuantity(),
                command.sellerId()
        );
        return ProductResult.from(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public ProductResult getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException("Product not found"));
        return ProductResult.from(product);
    }

    @Transactional
    public void changeProductStatus(Long productId, ProductStatus status) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException("Product not found"));
        product.changeStatus(status);
    }
}
