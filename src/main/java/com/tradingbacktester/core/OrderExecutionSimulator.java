package com.tradingbacktester.core;

import com.tradingbacktester.model.Bar;
import com.tradingbacktester.model.Order;
import com.tradingbacktester.model.OrderType;
import com.tradingbacktester.utils.Constants;

/**
 * Simulates order execution in the backtesting engine.
 */
public class OrderExecutionSimulator {
    private double commissionRate;
    private double slippageModel;
    
    /**
     * Creates a new order execution simulator with default settings.
     */
    public OrderExecutionSimulator() {
        this.commissionRate = Constants.DEFAULT_COMMISSION_RATE;
        this.slippageModel = Constants.DEFAULT_SLIPPAGE;
    }
    
    /**
     * Sets the commission rate.
     * 
     * @param commissionRate the commission rate (e.g., 0.001 for 0.1%)
     */
    public void setCommissionRate(double commissionRate) {
        this.commissionRate = commissionRate;
    }
    
    /**
     * Sets the slippage model.
     * 
     * @param slippageModel the slippage model value
     */
    public void setSlippageModel(double slippageModel) {
        this.slippageModel = slippageModel;
    }
    
    /**
     * Executes an order against the current market data.
     * 
     * @param order the order to execute
     * @param bar the current price bar
     * @param portfolio the portfolio to update
     * @return the resulting trade if the order is executed and completes a trade, null otherwise
     */
    public Trade executeOrder(Order order, Bar bar, Portfolio portfolio) {
        if (order == null || bar == null) {
            return null;
        }
        
        // Determine execution price based on order type and slippage
        double executionPrice = calculateExecutionPrice(order, bar);
        
        if (executionPrice <= 0) {
            // Order was not executed (e.g., limit order not filled)
            return null;
        }
        
        // Execute the order
        order.execute(bar.getTimestamp(), executionPrice);
        
        // Calculate commission
        double commission = Math.abs(order.getQuantity() * executionPrice * commissionRate);
        
        // Update portfolio
        boolean success = portfolio.updatePosition(
            order.getSymbol(), 
            order.getQuantity(), 
            executionPrice, 
            commission
        );
        
        if (!success) {
            return null; // Not enough capital or position to execute the order
        }
        
        // Check if this completes a trade (entry and exit)
        Position position = portfolio.getPosition(order.getSymbol());
        
        // If the position is closed, create a trade record
        if (position == null || position.getQuantity() == 0) {
            // This was an exit order
            // Find the original entry order and details
            Order entryOrder = null;
            double entryPrice = 0;
            double entryQuantity = 0;
            
            // Get the last transaction for this symbol
            for (int i = portfolio.getTransactions().size() - 2; i >= 0; i--) {
                Portfolio.Transaction transaction = portfolio.getTransactions().get(i);
                if (transaction.getSymbol().equals(order.getSymbol())) {
                    // Found the entry transaction
                    entryPrice = transaction.getPrice();
                    entryQuantity = transaction.getQuantity();
                    break;
                }
            }
            
            // Create the trade record
            boolean isLong = entryQuantity > 0;
            double profit = isLong ? 
                            (executionPrice - entryPrice) * Math.abs(entryQuantity) : 
                            (entryPrice - executionPrice) * Math.abs(entryQuantity);
            profit -= commission;
            
            double profitPercent = (profit / (entryPrice * Math.abs(entryQuantity))) * 100;
            
            return new Trade(
                order.getSymbol(),
                order.getCreationTime(), // Approximate entry time
                entryPrice,
                entryQuantity,
                order.getExecutionTime(),
                executionPrice,
                order.getQuantity(),
                commission,
                profit,
                profitPercent,
                isLong,
                portfolio.getTotalValue(),
                entryOrder,
                order
            );
        } else if (position.getOriginalOrder() == null) {
            // This is an entry order, set it as the original order for the position
            position.setOriginalOrder(order);
        }
        
        return null;
    }
    
    /**
     * Calculates the execution price based on order type and slippage.
     * 
     * @param order the order
     * @param bar the current price bar
     * @return the execution price, or 0 if the order is not executed
     */
    private double calculateExecutionPrice(Order order, Bar bar) {
        double basePrice;
        
        // Determine base price based on order type
        switch (order.getType()) {
            case MARKET:
                basePrice = order.isBuy() ? bar.getOpen() : bar.getOpen();
                break;
                
            case LIMIT:
                // For limit orders, check if the price threshold is reached
                if (order.isBuy() && bar.getLow() <= order.getExecutionPrice()) {
                    basePrice = order.getExecutionPrice();
                } else if (order.isSell() && bar.getHigh() >= order.getExecutionPrice()) {
                    basePrice = order.getExecutionPrice();
                } else {
                    return 0; // Order not executed
                }
                break;
                
            case STOP:
                // For stop orders, check if the stop price is reached
                if (order.isBuy() && bar.getHigh() >= order.getExecutionPrice()) {
                    basePrice = order.getExecutionPrice();
                } else if (order.isSell() && bar.getLow() <= order.getExecutionPrice()) {
                    basePrice = order.getExecutionPrice();
                } else {
                    return 0; // Order not executed
                }
                break;
                
            case STOP_LIMIT:
                // For stop-limit orders, check if both stop and limit conditions are met
                if (order.isBuy() && bar.getHigh() >= order.getExecutionPrice() && 
                    bar.getLow() <= order.getExecutionPrice()) {
                    basePrice = order.getExecutionPrice();
                } else if (order.isSell() && bar.getLow() <= order.getExecutionPrice() && 
                         bar.getHigh() >= order.getExecutionPrice()) {
                    basePrice = order.getExecutionPrice();
                } else {
                    return 0; // Order not executed
                }
                break;
                
            default:
                return 0; // Unknown order type
        }
        
        // Apply slippage
        if (order.isBuy()) {
            return basePrice * (1 + slippageModel);
        } else {
            return basePrice * (1 - slippageModel);
        }
    }
}
