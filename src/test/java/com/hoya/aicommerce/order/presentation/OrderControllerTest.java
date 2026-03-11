package com.hoya.aicommerce.order.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoya.aicommerce.order.application.OrderService;
import com.hoya.aicommerce.order.application.dto.OrderItemResult;
import com.hoya.aicommerce.order.application.dto.OrderResult;
import com.hoya.aicommerce.order.domain.OrderStatus;
import com.hoya.aicommerce.order.exception.OrderException;
import com.hoya.aicommerce.order.presentation.request.CreateOrderRequest;
import com.hoya.aicommerce.order.presentation.request.OrderItemRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private OrderService orderService;

    private OrderResult sampleOrderResult() {
        OrderItemResult item = new OrderItemResult(10L, "상품A", BigDecimal.valueOf(1000), 2);
        return new OrderResult(1L, 1L, OrderStatus.CREATED, BigDecimal.valueOf(2000), List.of(item));
    }

    @Test
    void 주문_생성_성공() throws Exception {
        given(orderService.createOrder(any())).willReturn(sampleOrderResult());

        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(new OrderItemRequest(10L, 2)));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memberId").value(1))
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.totalAmount").value(2000))
                .andExpect(jsonPath("$.data.items[0].productId").value(10));
    }

    @Test
    void 주문_생성_유효성_실패_항목_없음() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(1L, List.of());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 주문_생성_유효성_실패_memberId_없음() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":10,\"quantity\":1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 주문_단건_조회_성공() throws Exception {
        given(orderService.getOrder(1L)).willReturn(sampleOrderResult());

        mockMvc.perform(get("/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(1));
    }

    @Test
    void 존재하지_않는_주문_조회_실패() throws Exception {
        given(orderService.getOrder(99L)).willThrow(new OrderException("Order not found"));

        mockMvc.perform(get("/orders/99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Order not found"));
    }

    @Test
    void 주문_취소_성공() throws Exception {
        willDoNothing().given(orderService).cancelOrder(1L);

        mockMvc.perform(post("/orders/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 결제완료된_주문_취소_실패() throws Exception {
        willThrow(new OrderException("Cannot cancel a paid order")).given(orderService).cancelOrder(1L);

        mockMvc.perform(post("/orders/1/cancel"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cannot cancel a paid order"));
    }
}
