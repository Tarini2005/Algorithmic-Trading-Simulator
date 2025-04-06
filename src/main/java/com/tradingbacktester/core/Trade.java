package com.tradingbacktester.core;

import com.tradingbacktester.model.Order;
import java.time.LocalDateTime;

/**
 * Represents a completed trade (entry and exit).
 */
public class Trade {
    private final String symbol;
    private final LocalDateTime entryTime;
    private final double entryPrice;
    private final double entryQuantity;
    private final LocalDateTime exitTime;
    private final double exitPrice;
    private final double exitQuantity;
    private final double commission;
    private final double profit;
    private final double profitPercent;
    private final boolean isLong;
    private boolean isStopLoss;
    private boolean isTakeProfit;
    private final double capitalAfterTrade;
    private final Order entryOrder;
    private final Order exitOrder;
    
    /**
     * Creates a new trade.
     * 
     * @param symbol the symbol of the financial instrument
     * @param entryTime the entry time
     * @param entryPrice the entry price
     * @param entryQuantity the entry quantity
     * @param exitTime the exit time
     * @param exitPrice the exit price
     * @param exitQuantity the exit quantity
     * @param commission the total commission
     * @param profit the profit/loss
     * @param profitPercent the profit/loss percentage
     * @param isLong true if this is a long trade, false if it's a short trade
     * @param capitalAfterTrade the capital after the trade
     * @param entryOrder the entry order
     * @param exitOrder the exit order
     */
    public Trade(String symbol, LocalDateTime entryTime, double entryPrice, double entryQuantity,
                LocalDateTime exitTime, double exitPrice, double exitQuantity, double commission,
                double profit, double profitPercent, boolean isLong, double capitalAfterTrade,
                Order entryOrder, Order exitOrder) {
        this.symbol = symbol;
        this.entryTime = entryTime;
        this.entryPrice = entryPrice;
        this.entryQuantity = entryQuantity;
        this.exitTime = exitTime;
        this.exitPrice = exitPrice;
        this.exitQuantity = exitQuantity;
        this.commission = commission;
        this.profit = profit;
        this.profitPercent = profitPercent;
        this.isLong = isLong;
        this.capitalAfterTrade = capitalAfterTrade;
        this.entryOrder = entryOrder;
        this.exitOrder = exitOrder;
        this.isStopLoss = false;
        this.isTakeProfit = false;
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
     * Gets the entry time.
     * 
     * @return the entry time
     */
    public LocalDateTime getEntryTime() {
        return entryTime;
    }
    
    /**
     * Gets the entry price.
     * 
     * @return the entry price
     */
    public double getEntryPrice() {
        return entryPrice;
    }
    
    /**
     * Gets the entry quantity.
     * 
     * @return the entry quantity
     */
    public double getEntryQuantity() {
        return entryQuantity;
    }
    
    /**
     * Gets the exit time.
     * 
     * @return the exit time
     */
    public LocalDateTime getExitTime() {
        return exitTime;
    }
    
    /**
     * Gets the exit price.
     * 
     * @return the exit price
     */
    public double getExitPrice() {
        return exitPrice;
    }
    
    /**
     * Gets the exit quantity.
     * 
     * @return the exit quantity
     */
    public double getExitQuantity() {
        return exitQuantity;
    }
    
    /**
     * Gets the total commission.
     * 
     * @return the commission
     */
    public double getCommission() {
        return commission;
    }
    
    /**
     * Gets the profit/loss.
     * 
     * @return the profit/loss
     */
    public double getProfit() {
        return profit;
    }
    
    /**
     * Gets the profit/loss percentage.
     * 
     * @return the profit/loss percentage
     */
    public double getProfitPercent() {
        return profitPercent;
    }
    
    /**
     * Checks if this is a long trade.
     * 
     * @return true if this is a long trade, false if it's a short trade
     */
    public boolean isLong() {
        return isLong;
    }
    
    /**
     * Checks if this trade was closed by a stop loss.
     * 
     * @return true if this trade was closed by a stop loss, false otherwise
     */
    public boolean isStopLoss() {
        return isStopLoss;
    }
    
    /**
     * Sets whether this trade was closed by a stop loss.
     * 
     * @param isStopLoss true if this trade was closed by a stop loss, false otherwise
     */
    public void setStopLoss(boolean isStopLoss) {
        this.isStopLoss = isStopLoss;
    }
    
    /**
     * Checks if this trade was closed by a take profit.
     * 
     * @return true if this trade was closed by a take profit, false otherwise
     */
    public boolean isTakeProfit() {
        return isTakeProfit;
    }
    
    /**
     * Sets whether this trade was closed by a take profit.
     * 
     * @param isTakeProfit true if this trade was closed by a take profit, false otherwise
     */
    public void setTakeProfit(boolean isTakeProfit) {
        this.isTakeProfit = isTakeProfit;
    }
    
    /**
     * Gets the capital after the trade.
     * 
     * @return the capital after the trade
     */
    public double getCapitalAfterTrade() {
        return capitalAfterTrade;
    }
    
    /**
     * Gets the duration of the trade.
     * 
     * @return the duration in days
     */
    public long getDurationDays() {
        return java.time.Duration.between(entryTime, exitTime).toDays();
    }
    
    /**
     * Gets the entry order.
     * 
     * @return the entry order
     */
    public Order getEntryOrder() {
        return entryOrder;
    }
    
    /**
     * Gets the exit order.
     * 
     * @return the exit order
     */
    public Order getExitOrder() {
        return exitOrder;
    }
    
    @Override
    public String toString() {
        return "Trade{" +
                "symbol='" + symbol + '\'' +
                ", entryTime=" + entryTime +
                ", exitTime=" + exitTime +
                ", " + (isLong ? "LONG" : "SHORT") +
                ", profit=" + profit +
                ", profitPercent=" + profitPercent + "%" +
                (isStopLoss ? ", STOP LOSS" : "") +
                (isTakeProfit ? ", TAKE PROFIT" : "") +
                '}';
    }
}
