package com.hoya.aicommerce.cart.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoya.aicommerce.cart.application.CartService;
import com.hoya.aicommerce.cart.application.dto.CartItemResult;
import com.hoya.aicommerce.cart.application.dto.CartResult;
import com.hoya.aicommerce.cart.exception.CartException;
import com.hoya.aicommerce.cart.presentation.request.AddCartItemRequest;
import com.hoya.aicommerce.cart.presentation.request.UpdateCartItemQuantityRequest;
import com.hoya.aicommerce.common.auth.AuthContext;
import com.hoya.aicommerce.common.auth.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AuthContext authContext;

    @BeforeEach
    void setUp() {
        given(jwtProvider.validateToken("test-token")).willReturn(true);
        given(jwtProvider.getMemberId("test-token")).willReturn(1L);
        given(authContext.getMemberId()).willReturn(1L);
    }

    private CartResult emptyCart() {
        return new CartResult(1L, 1L, List.of());
    }

    private CartResult cartWithItem() {
        CartItemResult item = new CartItemResult(10L, "상품A", BigDecimal.valueOf(1000), 2);
        return new CartResult(1L, 1L, List.of(item));
    }

    @Test
    void 장바구니_조회_성공() throws Exception {
        given(cartService.getCart(1L)).willReturn(emptyCart());

        mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memberId").value(1))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void 장바구니_조회_인증_없으면_401() throws Exception {
        given(jwtProvider.validateToken(any())).willReturn(false);

        mockMvc.perform(get("/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 장바구니_조회_실패() throws Exception {
        given(cartService.getCart(1L)).willThrow(new CartException("Cart not found"));

        mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cart not found"));
    }

    @Test
    void 장바구니_상품_추가_성공() throws Exception {
        given(cartService.addItem(any())).willReturn(cartWithItem());

        AddCartItemRequest request = new AddCartItemRequest(10L, 2);

        mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].productId").value(10))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2));
    }

    @Test
    void 장바구니_상품_추가_유효성_실패() throws Exception {
        AddCartItemRequest request = new AddCartItemRequest(10L, 0);

        mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 장바구니_수량_변경_성공() throws Exception {
        CartItemResult updatedItem = new CartItemResult(10L, "상품A", BigDecimal.valueOf(1000), 5);
        given(cartService.updateItemQuantity(any())).willReturn(new CartResult(1L, 1L, List.of(updatedItem)));

        UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest(5);

        mockMvc.perform(patch("/cart/items/10")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].quantity").value(5));
    }

    @Test
    void 장바구니_상품_삭제_성공() throws Exception {
        willDoNothing().given(cartService).removeItem(1L, 10L);

        mockMvc.perform(delete("/cart/items/10")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
