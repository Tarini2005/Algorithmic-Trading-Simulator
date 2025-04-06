package com.tradingbacktester.core;

import com.tradingbacktester.model.Order;

/**
 * Represents a position in a financial instrument.
 */
public class Position {
    private final String symbol;
    private double quantity;
    private double avgPrice;
    private double currentPrice;
    private Order originalOrder;
    
    /**
     * Creates a new position.
     * 
     * @param symbol the symbol of the financial instrument
     * @param quantity the initial quantity
     * @param avgPrice the initial average price
     */
    public Position(String symbol, double quantity, double avgPrice) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.avgPrice = avgPrice;
        this.currentPrice = avgPrice;
    }
    
    /**
     * Creates a new position from an order.
     * 
     * @param symbol the symbol of the financial instrument
     * @param quantity the initial quantity
     * @param avgPrice the initial average price
     * @param order the original order that created this position
     */
    public Position(String symbol, double quantity, double avgPrice, Order order) {
        this(symbol, quantity, avgPrice);
        this.originalOrder = order;
    }
    
    /**
     * Updates the position with a new transaction.
     * 
     * @param deltaQuantity the change in quantity
     * @param price the transaction price
     */
    public void update(double deltaQuantity, double price) {
        if (deltaQuantity == 0) {
            return;
        }
        
        // If the position is being closed or reversed
        if ((quantity > 0 && quantity + deltaQuantity <= 0) || 
            (quantity < 0 && quantity + deltaQuantity >= 0)) {
            // Position is being closed or reversed
            avgPrice = price;
            quantity += deltaQuantity;
        } else if ((quantity > 0 && deltaQuantity > 0) || 
                  (quantity < 0 && deltaQuantity < 0)) {
            // Position is being increased
            double totalCost = Math.abs(quantity * avgPrice) + Math.abs(deltaQuantity * price);
            double totalQuantity = Math.abs(quantity) + Math.abs(deltaQuantity);
            avgPrice = totalCost / totalQuantity;
            quantity += deltaQuantity;
        } else {
            // Position is being reduced
            quantity += deltaQuantity;
        }
        
        currentPrice = price;
    }
    
    /**
     * Gets the symbol of the financial instrument.
     * 
     * @return the symbol
     */
    public String getSymbol() {
        return symbol;
    }
    
    /**
     * Gets the quantity of the position.
     * 
     * @return the quantity (positive for long, negative for short)
     */
    public double getQuantity() {
        return quantity;
    }
    
    /**
     * Gets the average price of the position.
     * 
     * @return the average price
     */
    public double getAvgPrice() {
        return avgPrice;
    }
    
    /**
     * Gets the current price of the position.
     * 
     * @return the current price
     */
    public double getCurrentPrice() {
        return currentPrice;
    }
    
    /**
     * Sets the current price of the position.
     * 
     * @param currentPrice the current price
     */
    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }
    
    /**
     * Gets the original order that created this position.
     * 
     * @return the original order, or null if not available
     */
    public Order getOriginalOrder() {
        return originalOrder;
    }
    
    /**
     * Sets the original order that created this position.
     * 
     * @param originalOrder the original order
     */
    public void setOriginalOrder(Order originalOrder) {
        this.originalOrder = originalOrder;
    }
    
    /**
     * Gets the value of the position (quantity * current price).
     * 
     * @return the value
     */
    public double getValue() {
        return quantity * currentPrice;
    }
    
    /**
     * Gets the unrealized profit/loss of the position.
     * 
     * @return the unrealized profit/loss
     */
    public double getUnrealizedPnL() {
        return (currentPrice - avgPrice) * quantity;
    }
    
    /**
     * Gets the unrealized profit/loss percentage of the position.
     * 
     * @return the unrealized profit/loss percentage
     */
    public double getUnrealizedPnLPercent() {
        if (avgPrice == 0) {
            return 0;
        }
        return ((currentPrice - avgPrice) / avgPrice) * 100 * (quantity > 0 ? 1 : -1);
    }
    
    /**
     * Checks if this is a long position.
     * 
     * @return true if this is a long position, false otherwise
     */
    public boolean isLong() {
        return quantity > 0;
    }
    
    /**
     * Checks if this is a short position.
     * 
     * @return true if this is a short position, false otherwise
     */
    public boolean isShort() {
        return quantity < 0;
    }
    
    @Override
    public String toString() {
        return "Position{" +
                "symbol='" + symbol + '\'' +
                ", quantity=" + quantity +
                ", avgPrice=" + avgPrice +
                ", currentPrice=" + currentPrice +
                ", pnl=" + getUnrealizedPnL() +
                ", pnlPct=" + getUnrealizedPnLPercent() + "%" +
                '}';
    }
}
