package com.hoya.aicommerce.order.application;

import com.hoya.aicommerce.catalog.domain.Product;
import com.hoya.aicommerce.catalog.domain.ProductRepository;
import com.hoya.aicommerce.catalog.exception.ProductException;
import com.hoya.aicommerce.order.application.dto.CreateOrderCommand;
import com.hoya.aicommerce.order.application.dto.OrderResult;
import com.hoya.aicommerce.order.domain.Order;
import com.hoya.aicommerce.order.domain.OrderRepository;
import com.hoya.aicommerce.order.exception.OrderException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional
    public OrderResult createOrder(CreateOrderCommand command) {
        Order order = Order.create(command.memberId());

        command.items().forEach(itemCmd -> {
            Product product = productRepository.findById(itemCmd.productId())
                    .orElseThrow(() -> new ProductException("Product not found"));

            if (!product.isOnSale()) {
                throw new ProductException("Product is not on sale: " + product.getId());
            }

            product.decreaseStock(itemCmd.quantity());
            order.addItem(product.getId(), product.getName(), product.getPrice(), itemCmd.quantity());
        });

        return OrderResult.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResult getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException("Order not found"));
        return OrderResult.from(order);
    }

    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException("Order not found"));
        order.cancel();
    }
}
