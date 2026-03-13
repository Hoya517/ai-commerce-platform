package com.hoya.aicommerce.order.presentation;

import com.hoya.aicommerce.common.auth.AuthContext;
import com.hoya.aicommerce.common.presentation.ApiResponse;
import com.hoya.aicommerce.order.application.OrderService;
import com.hoya.aicommerce.order.application.dto.CreateOrderCommand;
import com.hoya.aicommerce.order.application.dto.OrderItemCommand;
import com.hoya.aicommerce.order.presentation.request.CreateOrderRequest;
import com.hoya.aicommerce.order.presentation.response.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Order", description = "주문 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final AuthContext authContext;

    @Operation(summary = "주문 생성")
    @PostMapping
    public ApiResponse<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        List<OrderItemCommand> items = request.items().stream()
                .map(item -> new OrderItemCommand(item.productId(), item.quantity()))
                .toList();
        CreateOrderCommand command = new CreateOrderCommand(authContext.getMemberId(), items);
        return ApiResponse.success(OrderResponse.from(orderService.createOrder(command)));
    }

    @Operation(summary = "주문 단건 조회")
    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable Long orderId) {
        return ApiResponse.success(OrderResponse.from(orderService.getOrder(orderId)));
    }

    @Operation(summary = "주문 취소")
    @PostMapping("/{orderId}/cancel")
    public ApiResponse<Void> cancelOrder(@PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
        return ApiResponse.success(null);
    }
}
