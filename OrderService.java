package com.ecommerce.service;

import com.ecommerce.dto.OrderDto;
import com.ecommerce.entity.*;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CartService cartService;
    @Autowired private PaymentGatewayService paymentGatewayService;

    @Transactional
    public OrderDto.OrderResponse placeOrder(String username, OrderDto.PlaceOrderRequest request) {
        User user = findUser(username);
        Cart cart = cartService.getOrCreateCart(username);

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty. Add items before placing an order.");
        }

        // FIX 2 (MAJOR CODE SMELL): Validate ALL stock BEFORE processing payment.
        // Previously, payment was processed before stock check — a customer could be
        // charged even if stock was insufficient.
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            // Stock validation first — throw before any payment attempt
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new BadRequestException("Insufficient stock for: " + product.getName());
            }

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();
            orderItems.add(orderItem);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        // FIX 1 (MAJOR BUG): Use payment token from request DTO instead of hardcoded placeholder.
        // Previously: paymentGatewayService.processPayment(..., "payment_token_placeholder", ...)
        String txnId = paymentGatewayService.processPayment(
                request.getPaymentMethod(), request.getPaymentToken(), total);

        // FIX 3 (MAJOR CODE SMELL): Stock deduction is now inside @Transactional (already on this method).
        // If orderRepository.save(order) fails after stock deduction, the entire transaction
        // rolls back — stock is restored automatically.
        for (OrderItem oi : orderItems) {
            Product p = oi.getProduct();
            p.setStockQuantity(p.getStockQuantity() - oi.getQuantity());
            productRepository.save(p);
        }

        // Create and save order
        Order order = Order.builder()
                .user(user)
                .totalPrice(total)
                .shippingDetails(request.getShippingDetails())
                .status(OrderStatus.PAID)
                .build();
        order = orderRepository.save(order);

        for (OrderItem oi : orderItems) {
            oi.setOrder(order);
        }
        order.setItems(orderItems);
        order = orderRepository.save(order);

        // Clear cart
        cartService.clearCart(cart);

        return toOrderResponse(order);
    }

    public List<OrderDto.OrderResponse> getUserOrders(String username) {
        User user = findUser(username);
        return orderRepository.findByUser(user).stream()
                .map(this::toOrderResponse).collect(Collectors.toList());
    }

    public OrderDto.OrderResponse getOrderById(String username, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (!order.getUser().getUsername().equals(username)) {
            throw new BadRequestException("Order does not belong to the current user");
        }
        return toOrderResponse(order);
    }

    // Admin
    public List<OrderDto.OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::toOrderResponse).collect(Collectors.toList());
    }

    @Transactional
    public OrderDto.OrderResponse updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        order.setStatus(status);
        return toOrderResponse(orderRepository.save(order));
    }

    // -----------------------------------------------------------------------
    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private OrderDto.OrderResponse toOrderResponse(Order order) {
        List<OrderDto.OrderItemResponse> items = order.getItems().stream().map(oi -> {
            OrderDto.OrderItemResponse r = new OrderDto.OrderItemResponse();
            r.setProductId(oi.getProduct().getId());
            r.setProductName(oi.getProduct().getName());
            r.setQuantity(oi.getQuantity());
            r.setUnitPrice(oi.getUnitPrice());
            r.setSubtotal(oi.getUnitPrice().multiply(BigDecimal.valueOf(oi.getQuantity())));
            return r;
        }).collect(Collectors.toList());

        OrderDto.OrderResponse resp = new OrderDto.OrderResponse();
        resp.setId(order.getId());
        resp.setItems(items);
        resp.setTotalPrice(order.getTotalPrice());
        resp.setShippingDetails(order.getShippingDetails());
        resp.setStatus(order.getStatus());
        resp.setCreatedAt(order.getCreatedAt());
        resp.setUpdatedAt(order.getUpdatedAt());
        return resp;
    }
}
