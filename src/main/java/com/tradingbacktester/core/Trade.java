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
    
    public String getSymbol() {
        return symbol;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public double getEntryPrice() {
        return entryPrice;
    }

    public double getEntryQuantity() {
        return entryQuantity;
    }
    

    public LocalDateTime getExitTime() {
        return exitTime;
    }

    public double getExitPrice() {
        return exitPrice;
    }
    
    public double getExitQuantity() {
        return exitQuantity;
    }
    

    public double getCommission() {
        return commission;
    }
    

    public double getProfit() {
        return profit;
    }

    public double getProfitPercent() {
        return profitPercent;
    }
    

    public boolean isLong() {
        return isLong;
    }
    

    public boolean isStopLoss() {
        return isStopLoss;
    }
    
    public void setStopLoss(boolean isStopLoss) {
        this.isStopLoss = isStopLoss;
    }
    

    public boolean isTakeProfit() {
        return isTakeProfit;
    }

    public void setTakeProfit(boolean isTakeProfit) {
        this.isTakeProfit = isTakeProfit;
    }

    public double getCapitalAfterTrade() {
        return capitalAfterTrade;
    }

    public long getDurationDays() {
        return java.time.Duration.between(entryTime, exitTime).toDays();
    }

    public Order getEntryOrder() {
        return entryOrder;
    }
    

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
