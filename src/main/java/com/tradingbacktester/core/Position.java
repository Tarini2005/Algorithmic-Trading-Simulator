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
    
    public Position(String symbol, double quantity, double avgPrice) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.avgPrice = avgPrice;
        this.currentPrice = avgPrice;
    }
    

    public Position(String symbol, double quantity, double avgPrice, Order order) {
        this(symbol, quantity, avgPrice);
        this.originalOrder = order;
    }

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

    public String getSymbol() {
        return symbol;
    }

    public double getQuantity() {
        return quantity;
    }

    public double getAvgPrice() {
        return avgPrice;
    }
    

    public double getCurrentPrice() {
        return currentPrice;
    }
    
    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public Order getOriginalOrder() {
        return originalOrder;
    }

    public void setOriginalOrder(Order originalOrder) {
        this.originalOrder = originalOrder;
    }

    public double getValue() {
        return quantity * currentPrice;
    }
    

    public double getUnrealizedPnL() {
        return (currentPrice - avgPrice) * quantity;
    }
    
    public double getUnrealizedPnLPercent() {
        if (avgPrice == 0) {
            return 0;
        }
        return ((currentPrice - avgPrice) / avgPrice) * 100 * (quantity > 0 ? 1 : -1);
    }
    
    public boolean isLong() {
        return quantity > 0;
    }
    

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
