package com.ecommerce.service;

import com.ecommerce.dto.CartDto;
import com.ecommerce.entity.*;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired private CartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductService productService;

    public CartDto.CartResponse getCart(String username) {
        Cart cart = getOrCreateCart(username);
        return toCartResponse(cart);
    }

    @Transactional
    public CartDto.CartResponse addItem(String username, CartDto.AddItemRequest request) {
        Cart cart = getOrCreateCart(username);
        Product product = productService.findProductById(request.getProductId());

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new BadRequestException("Insufficient stock for product: " + product.getName());
        }

        CartItem existingItem = cartItemRepository.findByCartAndProduct(cart, product).orElse(null);
        if (existingItem != null) {
            int newQty = existingItem.getQuantity() + request.getQuantity();
            if (product.getStockQuantity() < newQty) {
                throw new BadRequestException("Insufficient stock for product: " + product.getName());
            }
            existingItem.setQuantity(newQty);
            cartItemRepository.save(existingItem);
        } else {
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cart.getItems().add(item);
            cartItemRepository.save(item);
        }

        return toCartResponse(cartRepository.findById(cart.getId()).orElseThrow());
    }

    @Transactional
    public CartDto.CartResponse updateItem(String username, Long itemId, CartDto.UpdateItemRequest request) {
        Cart cart = getOrCreateCart(username);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", itemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Cart item does not belong to your cart");
        }

        if (item.getProduct().getStockQuantity() < request.getQuantity()) {
            throw new BadRequestException("Insufficient stock");
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);
        return toCartResponse(cartRepository.findById(cart.getId()).orElseThrow());
    }

    @Transactional
    public CartDto.CartResponse removeItem(String username, Long itemId) {
        Cart cart = getOrCreateCart(username);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", itemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Cart item does not belong to your cart");
        }

        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        return toCartResponse(cartRepository.findById(cart.getId()).orElseThrow());
    }

    @Transactional
    public void clearCart(Cart cart) {
        cart.getItems().clear();
        cartItemRepository.deleteByCart(cart);
    }

    public Cart getOrCreateCart(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return cartRepository.findByUser(user).orElseGet(() -> {
            Cart newCart = Cart.builder().user(user).build();
            return cartRepository.save(newCart);
        });
    }

    private CartDto.CartResponse toCartResponse(Cart cart) {
        List<CartDto.CartItemResponse> itemResponses = cart.getItems().stream().map(item -> {
            CartDto.CartItemResponse r = new CartDto.CartItemResponse();
            r.setId(item.getId());
            r.setProductId(item.getProduct().getId());
            r.setProductName(item.getProduct().getName());
            r.setProductImageUrl(item.getProduct().getImageUrl());
            r.setUnitPrice(item.getProduct().getPrice());
            r.setQuantity(item.getQuantity());
            r.setSubtotal(item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            return r;
        }).collect(Collectors.toList());

        BigDecimal total = itemResponses.stream()
                .map(CartDto.CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CartDto.CartResponse response = new CartDto.CartResponse();
        response.setId(cart.getId());
        response.setItems(itemResponses);
        response.setTotalPrice(total);
        return response;
    }
}
