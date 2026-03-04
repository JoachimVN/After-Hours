package edu.ntnu.idatt2003.g23.model;

import java.math.BigDecimal;

// Represents a share of stock owned by an investor, including the stock details, quantity owned, and purchase price.

public class Share {
    private final Stock stock;
    private final BigDecimal quantity;
    private final BigDecimal purchasePrice;

    // Constructor for creating a new Share instance with the specified stock, quantity, and purchase price.
    public Share(Stock stock, BigDecimal quantity, BigDecimal purchasePrice) {
        this.stock = stock;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
    }

    /**
     * Getter for the stock associated with this share
     * @return stock associated with this share
     * @throws IllegalStateException if the stock is null
     */
    public Stock getStock() {
        if (stock == null) {
            throw new IllegalStateException("Stock is null");
        }
        return stock;
    }

    /**
     * Getter for the quantity of shares owned
     * @return quantity of shares owned
     * @throws IllegalStateException if the quantity is null or not positive
     */
    public BigDecimal getQuantity() {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Quantity is null or not positive");
        }
        return quantity;
    }

    /**
     * Getter for the purchase price of the shares
     * @return purchase price of the shares
     * @throws IllegalStateException if the purchase price is null or not positive
     */
    public BigDecimal getPurchasePrice() {
        if (purchasePrice == null || purchasePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Purchase price is null or not positive");
        }
        return purchasePrice;
    }
}
