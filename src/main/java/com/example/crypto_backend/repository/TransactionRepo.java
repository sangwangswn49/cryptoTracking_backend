package com.example.crypto_backend.repository;

import com.example.crypto_backend.model.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepo extends MongoRepository<Transaction, Integer> {
    Optional<List<Transaction>> getAllTransactionsByBuyerUserName(String userName);
    Optional<List<Transaction>> getAllTransactionsBySellerUserName(String userName);
}
