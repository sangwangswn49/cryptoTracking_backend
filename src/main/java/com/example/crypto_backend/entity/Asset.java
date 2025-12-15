package com.example.crypto_backend.entity;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class Asset {
    @Id
    private String assetId;
    private Double balance;
}
