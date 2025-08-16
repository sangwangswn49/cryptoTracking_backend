package com.example.crypto_backend.service;

import com.example.crypto_backend.Enum.OrderStatus;
import com.example.crypto_backend.Enum.OrderType;
import com.example.crypto_backend.model.*;
import com.example.crypto_backend.repository.OrderRepo;
import com.mongodb.MongoWriteException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {
    private final OrderRepo orderRepo;
    private final UserService userService;
    private final TransactionService transactionService;

    public OrderService(OrderRepo orderRepo, UserService userService, TransactionService transactionService) {
        this.orderRepo = orderRepo;
        this.userService = userService;
        this.transactionService = transactionService;
    }

    public Order createOrder(Order order) {
        try {
            User user = userService.getUserByUserName(order.getUserName());
            if (order.getType() == OrderType.BUY){
                // Check if user has enough USD money
                List<Asset> userAssets = user.getAssets();
                Asset usdAsset = userAssets.stream()
                        .filter(asset -> asset.getAssetId().equals("usd"))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("USD currency not found for user: " + user.getUserName()));
                if (usdAsset.getBalance() < order.getPrice() * order.getQuantity()) {
                    throw new RuntimeException("Not enough USD balance for the order");
                }
            } else if (order.getType() == OrderType.SELL) {
                // Check if user has enough coins
                List<Asset> userAssets = user.getAssets();
                Asset coinAsset = userAssets.stream()
                        .filter(asset -> asset.getAssetId().equals(order.getCoinId()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Coin not found for user: " + user.getUserName()));
                if (coinAsset.getBalance() < order.getQuantity()) {
                    throw new RuntimeException("Not enough coin balance for the order");
                }
            }
            if (order.getStatus() == OrderStatus.PENDING)
                checkAuction(order);
            order.setTimeStamp(LocalDateTime.now());
            return orderRepo.save(order);
        } catch (MongoWriteException e) {
            throw new RuntimeException("Error writing to the database", e);
        }
    }

    public Page<Order> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderRepo.findAll(pageable);
    }

    public List<Order> getAllOrdersByUserName(String userName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderRepo.getAllOrdersByUserName(userName, pageable)
                .orElseThrow(() -> new RuntimeException("User hasn't made any orders"));
    }

    private void checkAuction(Order newOrder){
        // Filter candidates list
        List<Order> candidates = orderRepo.findAll().stream()
                .filter(o -> !o.getUserName().equals(newOrder.getUserName()))
                .filter(o -> o.getCoinId().equals(newOrder.getCoinId()))
                .filter(o -> o.getStatus() == OrderStatus.PENDING)
                .filter(o -> o.getType() != newOrder.getType())
                .filter(o -> newOrder.getType() == OrderType.BUY ?
                        o.getPrice() <= newOrder.getPrice() : o.getPrice() >= newOrder.getPrice())
                .sorted((o1, o2) -> {
                    // Sort by price
                    int comp = newOrder.getType() == OrderType.BUY ?
                            o1.getPrice().compareTo(o2.getPrice()) :
                            o2.getPrice().compareTo(o1.getPrice());
                    // Sort by timestamp
                    if (comp != 0) return comp;
                    return o1.getTimeStamp().compareTo(o2.getTimeStamp());
                })
                .toList();
        System.out.println("candidates = " + candidates);
        if (candidates.isEmpty()) {
            System.out.println("No matching orders found.");
        }
        else {
            // Auction orders in candidates
            Double newOrderRemainQuant = newOrder.getQuantity();
            for (Order candidate : candidates) {
                if (newOrderRemainQuant == 0) break;

                Double tradePrice = newOrder.getPrice();
                if (newOrderRemainQuant >= candidate.getQuantity()) {
                    Double matchedQuant = candidate.getQuantity();
                    updateCompletedOrders(candidate, matchedQuant);
                    updateTransaction(matchedQuant, newOrder.getCoinId(), tradePrice, newOrder, candidate);
                    newOrderRemainQuant -= matchedQuant;
                }
                else {
                    Double matchedQuant = newOrderRemainQuant;
                    updatePartiallyCompletedOrders(candidate, matchedQuant);
                    updateTransaction(matchedQuant, newOrder.getCoinId(), tradePrice, newOrder, candidate);
                    newOrderRemainQuant = 0.0;
                }
            }
            if (newOrderRemainQuant == 0){
                updateCompletedOrders(newOrder, newOrder.getQuantity());
            }
            else {
                updatePartiallyCompletedOrders(newOrder, newOrder.getQuantity() - newOrderRemainQuant);
            }
        }
    }

    private void updateCompletedOrders(Order order, Double matchedQuant) {
        // Update order
        order.setStatus(OrderStatus.COMPLETED);
        createOrder(order);

        // Update user assets
        updateUserAsset(order, matchedQuant);
    }

    private void updatePartiallyCompletedOrders(Order order, Double matchedQuant){
        // Update partially completed order
        order.setStatus(OrderStatus.PARTIALLY_COMPLETED);
        createOrder(order);

        // Create a new order for the remaining quantity
        Order newOrder = new Order();
        newOrder.setQuantity(order.getQuantity() - matchedQuant);
        newOrder.setPrice(order.getPrice());
        newOrder.setCoinId(order.getCoinId());
        newOrder.setType(order.getType());
        newOrder.setUserName(order.getUserName());
        newOrder.setStatus(OrderStatus.PENDING);
        newOrder.setTimeStamp(LocalDateTime.now());
        createOrder(newOrder);

        // Update user assets
        updateUserAsset(order, matchedQuant);
    }

    private void updateUserAsset(Order order, Double matchedQuant){
        User user = userService.getUserByUserName(order.getUserName());
        List<Asset> userAssets = user.getAssets();

        // Update user's USD balance
        if (userAssets.stream().anyMatch(asset -> asset.getAssetId().equals("usd"))) {
            Asset money = userAssets.stream()
                    .filter(asset -> asset.getAssetId().equals("usd"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("USD currency not found for user: " + user.getUserName()));
            userAssets.remove(money);
            money.setBalance(order.getType().equals(OrderType.BUY) ?
                    money.getBalance() - order.getPrice() : money.getBalance() + order.getPrice());
            userAssets.add(money);
        }
        else {
            Asset newAsset = new Asset();
            newAsset.setAssetId("usd");
            newAsset.setBalance(order.getPrice());
            userAssets.add(newAsset);
        }

        // Update user's asset balance based on the order type
        if (userAssets.stream().anyMatch(asset -> asset.getAssetId().equals(order.getCoinId()))) {
            Asset newAsset = userAssets.stream()
                    .filter(asset -> asset.getAssetId().equals(order.getCoinId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Asset not found for user: " + user.getUserName()));
            userAssets.remove(newAsset);
            newAsset.setBalance(order.getType().equals(OrderType.BUY) ?
                    newAsset.getBalance() + matchedQuant : newAsset.getBalance() - matchedQuant);
            userAssets.add(newAsset);
        }
        else {
            Asset newAsset = new Asset();
            newAsset.setAssetId(order.getCoinId());
            newAsset.setBalance(matchedQuant);
            userAssets.add(newAsset);
        }

        user.setAssets(userAssets);
        userService.createUser(user);
    }

    private void updateTransaction (Double quant, String coinId, Double tradePrice, Order newOrder, Order counterOrder) {
        Transaction transaction = new Transaction();
        transaction.setQuantity(quant);
        transaction.setCoinId(coinId);
        transaction.setPrice(tradePrice);
        transaction.setBuyerUserName(
                newOrder.getType() == OrderType.BUY ? newOrder.getUserName() : counterOrder.getUserName());
        transaction.setSellerUserName(
                newOrder.getType() == OrderType.SELL ? newOrder.getUserName() : counterOrder.getUserName());
        transaction.setMatchingTimeStamp(LocalDateTime.now());
        transactionService.createTransaction(transaction);

        System.out.printf("Transaction done: %f %s %.2f $ (Buyer: %s, Seller: %s)%n",
                quant, coinId, tradePrice,
                newOrder.getType() == OrderType.BUY ? newOrder.getOrderId() : counterOrder.getOrderId(),
                newOrder.getType() == OrderType.SELL ? newOrder.getOrderId() : counterOrder.getOrderId()
        );
    }
}
