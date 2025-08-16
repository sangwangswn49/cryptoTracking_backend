package com.example.crypto_backend.model;

import com.example.crypto_backend.Enum.OrderStatus;
import com.example.crypto_backend.Enum.OrderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


import java.time.LocalDateTime;

@Document (collection = "order")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    private String orderId;
    private String coinId;
    private String userName;
    private OrderType type;
    private Double price;
    private Double quantity;
    private OrderStatus status;
    private LocalDateTime timeStamp;
}
