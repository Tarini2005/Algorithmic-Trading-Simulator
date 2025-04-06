package com.tradingbacktester.core;

import com.tradingbacktester.model.Bar;
import com.tradingbacktester.model.Order;
import com.tradingbacktester.model.OrderType;
import com.tradingbacktester.model.TimeSeries;
import com.tradingbacktester.strategy.Strategy;
import com.tradingbacktester.utils.Constants;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The main backtesting engine that simulates the execution of trading strategies on historical data.
 */
public class BacktestEngine {
    private final MarketDataService marketDataService;
    private final OrderExecutionSimulator orderExecutor;
    private final Portfolio portfolio;
    private final Map<String, Strategy> strategies;
    private final List<Trade> trades;
    private LocalDateTime currentDateTime;
    private double initialCapital;
    private double commissionRate;
    private double slippageModel;
    
    public BacktestEngine(MarketDataService marketDataService, double initialCapital) {
        this.marketDataService = marketDataService;
        this.orderExecutor = new OrderExecutionSimulator();
        this.portfolio = new Portfolio(initialCapital);
        this.strategies = new HashMap<>();
        this.trades = new ArrayList<>();
        this.initialCapital = initialCapital;
        this.commissionRate = Constants.DEFAULT_COMMISSION_RATE;
        this.slippageModel = Constants.DEFAULT_SLIPPAGE;
    }

    public void addStrategy(Strategy strategy) {
        strategies.put(strategy.getName(), strategy);
    }
    
    public void removeStrategy(String strategyName) {
        strategies.remove(strategyName);
    }
    
    public void setCommissionRate(double commissionRate) {
        this.commissionRate = commissionRate;
    }

    public void setSlippageModel(double slippage) {
        this.slippageModel = slippage;
    }
    
    public Map<String, Object> runBacktest(LocalDateTime startTime, LocalDateTime endTime) {
        // Reset the portfolio and trades
        portfolio.reset(initialCapital);
        trades.clear();
        
        // Set initial time
        currentDateTime = startTime;
        
        // Get all relevant market data
        Map<String, TimeSeries> allData = new HashMap<>();
        for (Strategy strategy : strategies.values()) {
            for (String symbol : strategy.getRequiredSymbols()) {
                if (!allData.containsKey(symbol)) {
                    TimeSeries data = marketDataService.getHistoricalData(symbol, startTime, endTime);
                    allData.put(symbol, data);
                }
            }
        }
        
        // Prepare strategies
        for (Strategy strategy : strategies.values()) {
            strategy.initialize(allData);
        }
        
        // Set up the order executor
        orderExecutor.setCommissionRate(commissionRate);
        orderExecutor.setSlippageModel(slippageModel);
        
        // Simulation loop - step through each bar
        List<LocalDateTime> allTimestamps = new ArrayList<>();
        for (TimeSeries series : allData.values()) {
            for (Bar bar : series.getBars()) {
                if (!allTimestamps.contains(bar.getTimestamp())) {
                    allTimestamps.add(bar.getTimestamp());
                }
            }
        }
        allTimestamps.sort(LocalDateTime::compareTo);
        
        for (LocalDateTime timestamp : allTimestamps) {
            if (timestamp.isBefore(startTime) || timestamp.isAfter(endTime)) {
                continue;
            }
            
            currentDateTime = timestamp;
            
            // Update market data
            Map<String, Bar> currentBars = new HashMap<>();
            for (Map.Entry<String, TimeSeries> entry : allData.entrySet()) {
                Bar bar = entry.getValue().getBar(timestamp);
                if (bar != null) {
                    currentBars.put(entry.getKey(), bar);
                }
            }
            
            // Check for stop losses and take profits
            checkStopLossAndTakeProfit(currentBars);
            
            // Process strategies
            for (Strategy strategy : strategies.values()) {
                strategy.onBar(currentDateTime, currentBars, portfolio);
                List<Order> orders = strategy.generateOrders(currentDateTime, currentBars, portfolio);
                
                // Execute orders
                for (Order order : orders) {
                    Trade trade = orderExecutor.executeOrder(order, currentBars.get(order.getSymbol()), portfolio);
                    if (trade != null) {
                        trades.add(trade);
                    }
                }
            }
        }
        
        // Calculate results
        return calculateResults();
    }
    
    private void checkStopLossAndTakeProfit(Map<String, Bar> currentBars) {
        for (Position position : portfolio.getPositions()) {
            String symbol = position.getSymbol();
            Bar bar = currentBars.get(symbol);
            
            if (bar == null) {
                continue;
            }
            
            Order order = position.getOriginalOrder();
            if (order.hasStopLoss()) {
                if ((position.isLong() && bar.getLow() <= order.getStopLossPrice()) ||
                    (position.isShort() && bar.getHigh() >= order.getStopLossPrice())) {
                    // Stop loss triggered
                    Order stopOrder = new Order(symbol, OrderType.MARKET, -position.getQuantity(), currentDateTime);
                    Trade trade = orderExecutor.executeOrder(stopOrder, bar, portfolio);
                    if (trade != null) {
                        trade.setStopLoss(true);
                        trades.add(trade);
                    }
                }
            }
            
            if (order.hasTakeProfit()) {
                if ((position.isLong() && bar.getHigh() >= order.getTakeProfitPrice()) ||
                    (position.isShort() && bar.getLow() <= order.getTakeProfitPrice())) {
                    // Take profit triggered
                    Order tpOrder = new Order(symbol, OrderType.MARKET, -position.getQuantity(), currentDateTime);
                    Trade trade = orderExecutor.executeOrder(tpOrder, bar, portfolio);
                    if (trade != null) {
                        trade.setTakeProfit(true);
                        trades.add(trade);
                    }
                }
            }
        }
    }
    
    private Map<String, Object> calculateResults() {
        Map<String, Object> results = new HashMap<>();
        
        // Basic performance metrics
        double finalCapital = portfolio.getTotalValue();
        double profit = finalCapital - initialCapital;
        double returnPct = (profit / initialCapital) * 100;
        
        results.put("initialCapital", initialCapital);
        results.put("finalCapital", finalCapital);
        results.put("profit", profit);
        results.put("returnPct", returnPct);
        results.put("trades", trades);
        
        // Trading statistics
        int totalTrades = trades.size();
        int winningTrades = 0;
        int losingTrades = 0;
        double totalProfit = 0;
        double totalLoss = 0;
        
        for (Trade trade : trades) {
            if (trade.getProfit() > 0) {
                winningTrades++;
                totalProfit += trade.getProfit();
            } else {
                losingTrades++;
                totalLoss += Math.abs(trade.getProfit());
            }
        }
        
        double winRate = totalTrades > 0 ? (double) winningTrades / totalTrades * 100 : 0;
        double averageProfit = winningTrades > 0 ? totalProfit / winningTrades : 0;
        double averageLoss = losingTrades > 0 ? totalLoss / losingTrades : 0;
        double profitFactor = totalLoss > 0 ? totalProfit / totalLoss : 0;
        
        results.put("totalTrades", totalTrades);
        results.put("winningTrades", winningTrades);
        results.put("losingTrades", losingTrades);
        results.put("winRate", winRate);
        results.put("averageProfit", averageProfit);
        results.put("averageLoss", averageLoss);
        results.put("profitFactor", profitFactor);
        
        // Calculate drawdown
        double maxDrawdown = calculateMaxDrawdown();
        results.put("maxDrawdown", maxDrawdown);
        
        return results;
    }
    
    private double calculateMaxDrawdown() {
        double maxCapital = initialCapital;
        double maxDrawdown = 0;
        
        for (Trade trade : trades) {
            double capitalAfterTrade = trade.getCapitalAfterTrade();
            maxCapital = Math.max(maxCapital, capitalAfterTrade);
            
            double drawdown = (maxCapital - capitalAfterTrade) / maxCapital * 100;
            maxDrawdown = Math.max(maxDrawdown, drawdown);
        }
        
        return maxDrawdown;
    }
    
    public List<Trade> getTrades() {
        return new ArrayList<>(trades);
    }
    
    public Portfolio getPortfolio() {
        return portfolio;
    }
}
