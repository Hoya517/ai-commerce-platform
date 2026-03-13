package com.hoya.aicommerce.order.application;

import com.hoya.aicommerce.cart.domain.Cart;
import com.hoya.aicommerce.cart.domain.CartRepository;
import com.hoya.aicommerce.cart.exception.CartException;
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
    private final CartRepository cartRepository;

    @Transactional
    public OrderResult createOrder(CreateOrderCommand command) {
        Order order = Order.create(command.memberId());

        command.items().forEach(itemCmd -> {
            Product product = productRepository.findByIdWithLock(itemCmd.productId())
                    .orElseThrow(() -> new ProductException("Product not found"));

            if (!product.isOnSale()) {
                throw new ProductException("Product is not on sale: " + product.getId());
            }

            product.decreaseStock(itemCmd.quantity());
            order.addItem(product.getId(), product.getName(), product.getPrice(), itemCmd.quantity());
        });

        return OrderResult.from(orderRepository.save(order));
    }

    @Transactional
    public OrderResult createOrderFromCart(Long memberId) {
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CartException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new CartException("Cart is empty");
        }

        Order order = Order.create(memberId);

        cart.getItems().forEach(cartItem -> {
            Product product = productRepository.findByIdWithLock(cartItem.getProductId())
                    .orElseThrow(() -> new ProductException("Product not found"));

            if (!product.isOnSale()) {
                throw new ProductException("Product is not on sale: " + product.getId());
            }

            product.decreaseStock(cartItem.getQuantity());
            order.addItem(product.getId(), product.getName(), product.getPrice(), cartItem.getQuantity());
        });

        OrderResult result = OrderResult.from(orderRepository.save(order));
        cart.clear();
        return result;
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
        order.getItems().forEach(item ->
                productRepository.findByIdWithLock(item.getProductId())
                        .ifPresent(product -> product.increaseStock(item.getQuantity()))
        );
    }
}
