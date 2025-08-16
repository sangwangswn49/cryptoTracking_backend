package com.example.crypto_backend.service;

import com.example.crypto_backend.model.Transaction;
import com.example.crypto_backend.repository.TransactionRepo;
import com.mongodb.MongoWriteException;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TransactionService {
    private final TransactionRepo transactionRepo;

    public TransactionService(TransactionRepo transactionRepo){
        this.transactionRepo = transactionRepo;
    }

    public Transaction createTransaction(Transaction transaction){
        try {
            return transactionRepo.save(transaction);
        } catch (MongoWriteException e) {
            throw new RuntimeException("Error writing to the database", e);
        }
    }

    public List<Transaction> getAllTransactionsByUserName(String userName){
        List<Transaction> buyerTransactions = transactionRepo.getAllTransactionsByBuyerUserName(userName).orElse(null);
        if (buyerTransactions != null && !buyerTransactions.isEmpty()) {
            return buyerTransactions;
        }
        List<Transaction> sellerTransactions = transactionRepo.getAllTransactionsBySellerUserName(userName).orElse(null);
        if (sellerTransactions != null && !sellerTransactions.isEmpty()) {
            return sellerTransactions;
        }
        throw new RuntimeException("User hasn't made any transactions");
    }

    public List<Transaction> getAllTransactions(){
        return transactionRepo.findAll();
    }
}
