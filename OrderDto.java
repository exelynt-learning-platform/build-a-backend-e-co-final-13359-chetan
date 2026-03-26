package com.ecommerce.dto;

import com.ecommerce.entity.OrderStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDto {

    @Data
    public static class PlaceOrderRequest {
        @NotBlank(message = "Shipping details are required")
        private String shippingDetails;

        @NotBlank(message = "Payment method is required (STRIPE or PAYPAL)")
        private String paymentMethod;
    }

    @Data
    public static class OrderItemResponse {
        private Long productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }

    @Data
    public static class OrderResponse {
        private Long id;
        private List<OrderItemResponse> items;
        private BigDecimal totalPrice;
        private String shippingDetails;
        private OrderStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class PaymentRequest {
        @NotBlank(message = "Payment token is required")
        private String paymentToken;
    }
}
