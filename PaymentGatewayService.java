package com.ecommerce.service;

import com.ecommerce.exception.PaymentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Payment Gateway abstraction — supports Stripe and PayPal.
 * Replace the stub logic below with real Stripe/PayPal SDK calls
 * once the API keys are configured.
 */
@Service
public class PaymentGatewayService {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayService.class);

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${paypal.client.id}")
    private String paypalClientId;

    @Value("${paypal.client.secret}")
    private String paypalClientSecret;

    @Value("${paypal.mode}")
    private String paypalMode;

    /**
     * Process payment through the given gateway.
     *
     * @param paymentMethod STRIPE or PAYPAL
     * @param paymentToken  Token / nonce returned from frontend SDK
     * @param amount        Amount in the smallest currency unit (e.g. cents for USD)
     * @return transaction ID
     */
    public String processPayment(String paymentMethod, String paymentToken, BigDecimal amount) {
        return switch (paymentMethod.toUpperCase()) {
            case "STRIPE" -> processStripe(paymentToken, amount);
            case "PAYPAL" -> processPayPal(paymentToken, amount);
            default -> throw new PaymentException("Unsupported payment method: " + paymentMethod);
        };
    }

    // -----------------------------------------------------------------------
    // Stripe
    // -----------------------------------------------------------------------
    private String processStripe(String token, BigDecimal amount) {
        try {
            log.info("Processing Stripe payment: token={}, amount={}", token, amount);
            /*
             * Real implementation (add stripe-java dependency):
             *
             * Stripe.apiKey = stripeApiKey;
             * ChargeCreateParams params = ChargeCreateParams.builder()
             *         .setAmount(amount.multiply(BigDecimal.valueOf(100)).longValue())
             *         .setCurrency("usd")
             *         .setSource(token)
             *         .build();
             * Charge charge = Charge.create(params);
             * if (!"succeeded".equals(charge.getStatus())) {
             *     throw new PaymentException("Stripe payment failed: " + charge.getFailureMessage());
             * }
             * return charge.getId();
             */
            // Stub: simulate success
            return "stripe_txn_" + System.currentTimeMillis();
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Stripe payment error", e);
            throw new PaymentException("Stripe payment failed: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // PayPal
    // -----------------------------------------------------------------------
    private String processPayPal(String token, BigDecimal amount) {
        try {
            log.info("Processing PayPal payment: token={}, amount={}", token, amount);
            /*
             * Real implementation (add paypal-checkout-sdk dependency):
             *
             * PayPalHttpClient client = new PayPalHttpClient(
             *     new PayPalEnvironment.Sandbox(paypalClientId, paypalClientSecret));
             * OrdersCaptureRequest request = new OrdersCaptureRequest(token);
             * HttpResponse<Order> response = client.execute(request);
             * if (!"COMPLETED".equals(response.result().status())) {
             *     throw new PaymentException("PayPal payment not completed");
             * }
             * return response.result().id();
             */
            // Stub: simulate success
            return "paypal_txn_" + System.currentTimeMillis();
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("PayPal payment error", e);
            throw new PaymentException("PayPal payment failed: " + e.getMessage());
        }
    }
}
