package com.hoya.aicommerce.catalog.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoya.aicommerce.catalog.application.ProductService;
import com.hoya.aicommerce.common.auth.AuthContext;
import com.hoya.aicommerce.common.auth.JwtProvider;
import com.hoya.aicommerce.catalog.application.dto.ProductResult;
import com.hoya.aicommerce.catalog.domain.ProductStatus;
import com.hoya.aicommerce.catalog.exception.ProductException;
import com.hoya.aicommerce.catalog.presentation.request.ChangeProductStatusRequest;
import com.hoya.aicommerce.catalog.presentation.request.CreateProductRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AuthContext authContext;

    private ProductResult sampleResult() {
        return new ProductResult(1L, "상품A", "설명", BigDecimal.valueOf(1000), 10, ProductStatus.ON_SALE);
    }

    @Test
    void 상품_생성_성공() throws Exception {
        given(productService.createProduct(any())).willReturn(sampleResult());

        CreateProductRequest request = new CreateProductRequest("상품A", "설명", BigDecimal.valueOf(1000), 10);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("상품A"))
                .andExpect(jsonPath("$.data.status").value("ON_SALE"));
    }

    @Test
    void 상품_생성_유효성_실패() throws Exception {
        CreateProductRequest request = new CreateProductRequest("", "설명", BigDecimal.valueOf(1000), 10);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 상품_단건_조회_성공() throws Exception {
        given(productService.getProduct(1L)).willReturn(sampleResult());

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("상품A"));
    }

    @Test
    void 존재하지_않는_상품_조회_실패() throws Exception {
        given(productService.getProduct(99L)).willThrow(new ProductException("Product not found"));

        mockMvc.perform(get("/products/99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Product not found"));
    }

    @Test
    void 상품_상태_변경_성공() throws Exception {
        willDoNothing().given(productService).changeProductStatus(eq(1L), any());

        ChangeProductStatusRequest request = new ChangeProductStatusRequest(ProductStatus.HIDDEN);

        mockMvc.perform(patch("/products/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 상품_상태_변경_유효성_실패() throws Exception {
        mockMvc.perform(patch("/products/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 예상치_못한_예외_발생시_500_반환() throws Exception {
        given(productService.getProduct(any())).willThrow(new RuntimeException("unexpected error"));

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("unexpected error"));
    }
}
