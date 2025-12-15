package com.example.crypto_backend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document (collection = "transaction")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    private String transactionId;
    private String sellerUserName;
    private String buyerUserName;
    private Double quantity;
    private String coinId;
    private Double price;
    private LocalDateTime matchingTimeStamp;
}
