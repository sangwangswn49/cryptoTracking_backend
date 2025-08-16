package com.example.crypto_backend.repository;

import com.example.crypto_backend.model.Order;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepo extends MongoRepository<Order, Integer> {
    Optional<List<Order>> getAllOrdersByUserName(String userName, Pageable pageable);
}
