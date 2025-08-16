package com.example.crypto_backend.controller;

import com.example.crypto_backend.model.Order;
import com.example.crypto_backend.service.OrderService;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.util.Objects;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService){
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Order order, UriComponentsBuilder ucb) {
        try{
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!authentication.getName().equals(order.getUserName())) {
                return ResponseEntity.status(403).body("You can only create orders for your own account.");
            }
            Order createdOrder =  orderService.createOrder(order);
            URI locationOfNewOrder = ucb
                    .path("/orders/{id}")
                    .buildAndExpand(createdOrder.getOrderId())
                    .toUri();
            System.out.println(ResponseEntity.created(locationOfNewOrder).build());
            return ResponseEntity.created(locationOfNewOrder).build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllOrders(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "2") int size) {
        try{
            return ResponseEntity.ok(orderService.getAllOrders(page, size));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PreAuthorize("#userName == authentication.name or hasRole('ROLE_ADMIN')")
    @GetMapping("/{userName}")
    public ResponseEntity<?> getOrdersByUserName(@PathVariable String userName,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "2") int size) {
        try {
            return ResponseEntity.ok(orderService.getAllOrdersByUserName(userName, page, size));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
}
