package com.tradingbacktester.model;

import java.time.LocalDateTime;

/**
 * Represents a trading order in the backtesting system.
 */
public class Order {
    private final String symbol;
    private final OrderType type;
    private final double quantity;
    private final LocalDateTime creationTime;
    private LocalDateTime executionTime;
    private double executionPrice;
    private double stopLossPrice;
    private double takeProfitPrice;
    private boolean isExecuted;
    private final String id;
    private static long nextId = 1;
    
    /**
     * Creates a new order.
     * 
     * @param symbol the symbol of the financial instrument
     * @param type the order type
     * @param quantity the quantity to buy or sell (positive for buy, negative for sell)
     * @param creationTime the time when the order was created
     */
    public Order(String symbol, OrderType type, double quantity, LocalDateTime creationTime) {
        this.symbol = symbol;
        this.type = type;
        this.quantity = quantity;
        this.creationTime = creationTime;
        this.isExecuted = false;
        this.id = generateId();
    }
    
    /**
     * Creates a new order with stop loss and take profit levels.
     * 
     * @param symbol the symbol of the financial instrument
     * @param type the order type
     * @param quantity the quantity to buy or sell (positive for buy, negative for sell)
     * @param creationTime the time when the order was created
     * @param stopLossPrice the stop loss price
     * @param takeProfitPrice the take profit price
     */
    public Order(String symbol, OrderType type, double quantity, LocalDateTime creationTime, 
                 double stopLossPrice, double takeProfitPrice) {
        this(symbol, type, quantity, creationTime);
        this.stopLossPrice = stopLossPrice;
        this.takeProfitPrice = takeProfitPrice;
    }
    
    private static synchronized String generateId() {
        return "ORD-" + (nextId++);
    }
    
    /**
     * Executes the order at the specified time and price.
     * 
     * @param executionTime the time of execution
     * @param executionPrice the execution price
     */
    public void execute(LocalDateTime executionTime, double executionPrice) {
        this.executionTime = executionTime;
        this.executionPrice = executionPrice;
        this.isExecuted = true;
    }
    
    /**
     * Checks if this order is a buy order.
     * 
     * @return true if this is a buy order, false otherwise
     */
    public boolean isBuy() {
        return quantity > 0;
    }
    
    /**
     * Checks if this order is a sell order.
     * 
     * @return true if this is a sell order, false otherwise
     */
    public boolean isSell() {
        return quantity < 0;
    }
    
    /**
     * Checks if this order has a stop loss level.
     * 
     * @return true if this order has a stop loss level, false otherwise
     */
    public boolean hasStopLoss() {
        return stopLossPrice > 0;
    }
    
    /**
     * Checks if this order has a take profit level.
     * 
     * @return true if this order has a take profit level, false otherwise
     */
    public boolean hasTakeProfit() {
        return takeProfitPrice > 0;
    }
    
    /**
     * Gets the order ID.
     * 
     * @return the order ID
     */
    public String getId() {
        return id;
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
     * Gets the order type.
     * 
     * @return the order type
     */
    public OrderType getType() {
        return type;
    }
    
    /**
     * Gets the order quantity.
     * 
     * @return the quantity (positive for buy, negative for sell)
     */
    public double getQuantity() {
        return quantity;
    }
    
    /**
     * Gets the absolute quantity (always positive).
     * 
     * @return the absolute quantity
     */
    public double getAbsoluteQuantity() {
        return Math.abs(quantity);
    }
    
    /**
     * Gets the creation time.
     * 
     * @return the creation time
     */
    public LocalDateTime getCreationTime() {
        return creationTime;
    }
    
    /**
     * Gets the execution time.
     * 
     * @return the execution time, or null if the order is not executed
     */
    public LocalDateTime getExecutionTime() {
        return executionTime;
    }
    
    /**
     * Gets the execution price.
     * 
     * @return the execution price, or 0 if the order is not executed
     */
    public double getExecutionPrice() {
        return executionPrice;
    }
    
    /**
     * Gets the stop loss price.
     * 
     * @return the stop loss price, or 0 if not set
     */
    public double getStopLossPrice() {
        return stopLossPrice;
    }
    
    /**
     * Gets the take profit price.
     * 
     * @return the take profit price, or 0 if not set
     */
    public double getTakeProfitPrice() {
        return takeProfitPrice;
    }
    
    /**
     * Checks if the order is executed.
     * 
     * @return true if the order is executed, false otherwise
     */
    public boolean isExecuted() {
        return isExecuted;
    }
    
    /**
     * Sets the stop loss price.
     * 
     * @param stopLossPrice the stop loss price
     */
    public void setStopLossPrice(double stopLossPrice) {
        this.stopLossPrice = stopLossPrice;
    }
    
    /**
     * Sets the take profit price.
     * 
     * @param takeProfitPrice the take profit price
     */
    public void setTakeProfitPrice(double takeProfitPrice) {
        this.takeProfitPrice = takeProfitPrice;
    }
    
    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' +
                ", symbol='" + symbol + '\'' +
                ", type=" + type +
                ", quantity=" + quantity +
                ", isExecuted=" + isExecuted +
                (isExecuted ? ", executionPrice=" + executionPrice : "") +
                '}';
    }
}
