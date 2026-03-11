package com.hoya.aicommerce.catalog.presentation;

import com.hoya.aicommerce.catalog.application.ProductService;
import com.hoya.aicommerce.catalog.application.dto.CreateProductCommand;
import com.hoya.aicommerce.common.presentation.ApiResponse;
import com.hoya.aicommerce.catalog.presentation.request.ChangeProductStatusRequest;
import com.hoya.aicommerce.catalog.presentation.request.CreateProductRequest;
import com.hoya.aicommerce.catalog.presentation.response.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Product", description = "상품 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "상품 등록")
    @PostMapping
    public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        CreateProductCommand command = new CreateProductCommand(
                request.name(), request.description(), request.price(), request.stockQuantity()
        );
        return ApiResponse.success(ProductResponse.from(productService.createProduct(command)));
    }

    @Operation(summary = "상품 단건 조회")
    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable Long productId) {
        return ApiResponse.success(ProductResponse.from(productService.getProduct(productId)));
    }

    @Operation(summary = "상품 상태 변경")
    @PatchMapping("/{productId}/status")
    public ApiResponse<Void> changeProductStatus(
            @PathVariable Long productId,
            @Valid @RequestBody ChangeProductStatusRequest request
    ) {
        productService.changeProductStatus(productId, request.status());
        return ApiResponse.success(null);
    }
}
