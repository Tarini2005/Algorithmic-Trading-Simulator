package com.tradingbacktester.strategy;

import com.tradingbacktester.core.Portfolio;
import com.tradingbacktester.indicators.RSI;
import com.tradingbacktester.model.Bar;
import com.tradingbacktester.model.Order;
import com.tradingbacktester.model.OrderType;
import com.tradingbacktester.model.TimeSeries;

import java.time.LocalDateTime;
import java.util.*;

/**
 * RSI Strategy.
 * Generates buy signals when RSI falls below the oversold level and then rises above it,
 * and sell signals when RSI rises above the overbought level and then falls below it.
 */
public class RSIStrategy implements Strategy {
    private final String symbol;
    private int period;
    private double oversoldLevel;
    private double overboughtLevel;
    private double positionSize;
    private double stopLossPercent;
    private double takeProfitPercent;
    
    private Map<String, TimeSeries> data;
    private List<Double> rsiValues;
    private boolean wasOversold;
    private boolean wasOverbought;
    
    /**
     * Creates a new RSI strategy.
     * 
     * @param symbol the symbol to trade
     * @param period the period of the RSI indicator
     * @param oversoldLevel the oversold level (e.g., 30)
     * @param overboughtLevel the overbought level (e.g., 70)
     * @param positionSize the position size as a percentage of the portfolio value
     * @param stopLossPercent the stop loss percentage (0 for no stop loss)
     * @param takeProfitPercent the take profit percentage (0 for no take profit)
     */
    public RSIStrategy(String symbol, int period, double oversoldLevel, double overboughtLevel,
                      double positionSize, double stopLossPercent, double takeProfitPercent) {
        this.symbol = symbol;
        this.period = period;
        this.oversoldLevel = oversoldLevel;
        this.overboughtLevel = overboughtLevel;
        this.positionSize = positionSize;
        this.stopLossPercent = stopLossPercent;
        this.takeProfitPercent = takeProfitPercent;
        this.wasOversold = false;
        this.wasOverbought = false;
    }
    
    @Override
    public String getName() {
        return "RSI Strategy (RSI(" + period + "), " + oversoldLevel + ", " + overboughtLevel + ")";
    }
    
    @Override
    public Set<String> getRequiredSymbols() {
        return Collections.singleton(symbol);
    }
    
    @Override
    public void initialize(Map<String, TimeSeries> data) {
        this.data = data;
        TimeSeries series = data.get(symbol);
        
        if (series == null) {
            throw new IllegalArgumentException("No data available for symbol: " + symbol);
        }
        
        // Calculate initial RSI values
        RSI rsi = new RSI(period);
        rsiValues = rsi.calculate(series);
        
        // Initialize oversold/overbought state
        int lastValidIndex = findLastValidIndex();
        if (lastValidIndex >= 0) {
            double lastRsi = rsiValues.get(lastValidIndex);
            wasOversold = lastRsi <= oversoldLevel;
            wasOverbought = lastRsi >= overboughtLevel;
        }
    }
    
    /**
     * Finds the last index where RSI has a valid value.
     * 
     * @return the last valid index, or -1 if none
     */
    private int findLastValidIndex() {
        for (int i = rsiValues.size() - 1; i >= 0; i--) {
            if (!Double.isNaN(rsiValues.get(i))) {
                return i;
            }
        }
        return -1;
    }
    
    @Override
    public void onBar(LocalDateTime timestamp, Map<String, Bar> currentBars, Portfolio portfolio) {
        Bar bar = currentBars.get(symbol);
        if (bar == null) {
            return;
        }
        
        TimeSeries series = data.get(symbol);
        
        // Update RSI values
        RSI rsi = new RSI(period);
        rsiValues = rsi.calculate(series);
    }
    
    @Override
    public List<Order> generateOrders(LocalDateTime timestamp, Map<String, Bar> currentBars, Portfolio portfolio) {
        Bar bar = currentBars.get(symbol);
        if (bar == null) {
            return Collections.emptyList();
        }
        
        int lastIndex = rsiValues.size() - 1;
        if (lastIndex < 0 || Double.isNaN(rsiValues.get(lastIndex))) {
            return Collections.emptyList();
        }
        
        double currentRsi = rsiValues.get(lastIndex);
        List<Order> orders = new ArrayList<>();
        
        // Check for oversold/overbought conditions
        boolean isOversold = currentRsi <= oversoldLevel;
        boolean isOverbought = currentRsi >= overboughtLevel;
        
        // Buy signal: RSI was oversold and is now rising above the oversold level
        if (wasOversold && !isOversold) {
            // Close any existing short position
            if (portfolio.hasPosition(symbol) && portfolio.getPosition(symbol).isShort()) {
                orders.add(new Order(symbol, OrderType.MARKET, -portfolio.getPosition(symbol).getQuantity(), timestamp));
            }
            
            // Open long position if we don't already have one
            if (!portfolio.hasPosition(symbol) || !portfolio.getPosition(symbol).isLong()) {
                double price = bar.getClose();
                double positionValue = portfolio.getTotalValue() * positionSize;
                int quantity = (int) (positionValue / price);
                
                if (quantity > 0) {
                    Order order = new Order(symbol, OrderType.MARKET, quantity, timestamp);
                    
                    // Set stop loss and take profit
                    if (stopLossPercent > 0) {
                        double stopPrice = price * (1 - stopLossPercent / 100);
                        order.setStopLossPrice(stopPrice);
                    }
                    
                    if (takeProfitPercent > 0) {
                        double tpPrice = price * (1 + takeProfitPercent / 100);
                        order.setTakeProfitPrice(tpPrice);
                    }
                    
                    orders.add(order);
                }
            }
        }
        
        // Sell signal: RSI was overbought and is now falling below the overbought level
        else if (wasOverbought && !isOverbought) {
            // Close any existing long position
            if (portfolio.hasPosition(symbol) && portfolio.getPosition(symbol).isLong()) {
                orders.add(new Order(symbol, OrderType.MARKET, -portfolio.getPosition(symbol).getQuantity(), timestamp));
            }
            
            // Open short position if enabled
            // Note: Uncommenting the following code would enable short selling
            /*
            if (!portfolio.hasPosition(symbol) || !portfolio.getPosition(symbol).isShort()) {
                double price = bar.getClose();
                double positionValue = portfolio.getTotalValue() * positionSize;
                int quantity = (int) (positionValue / price);
                
                if (quantity > 0) {
                    Order order = new Order(symbol, OrderType.MARKET, -quantity, timestamp);
                    
                    // Set stop loss and take profit
                    if (stopLossPercent > 0) {
                        double stopPrice = price * (1 + stopLossPercent / 100);
                        order.setStopLossPrice(stopPrice);
                    }
                    
                    if (takeProfitPercent > 0) {
                        double tpPrice = price * (1 - takeProfitPercent / 100);
                        order.setTakeProfitPrice(tpPrice);
                    }
                    
                    orders.add(order);
                }
            }
            */
        }
        
        // Update state
        wasOversold = isOversold;
        wasOverbought = isOverbought;
        
        return orders;
    }
    
    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("period", period);
        params.put("oversoldLevel", oversoldLevel);
        params.put("overboughtLevel", overboughtLevel);
        params.put("positionSize", positionSize);
        params.put("stopLossPercent", stopLossPercent);
        params.put("takeProfitPercent", takeProfitPercent);
        return params;
    }
    
    @Override
    public void setParameter(String name, Object value) {
        switch (name) {
            case "period":
                if (value instanceof Integer) {
                    this.period = (Integer) value;
                }
                break;
            case "oversoldLevel":
                if (value instanceof Double) {
                    this.oversoldLevel = (Double) value;
                } else if (value instanceof Integer) {
                    this.oversoldLevel = ((Integer) value).doubleValue();
                }
                break;
            case "overboughtLevel":
                if (value instanceof Double) {
                    this.overboughtLevel = (Double) value;
                } else if (value instanceof Integer) {
                    this.overboughtLevel = ((Integer) value).doubleValue();
                }
                break;
            case "positionSize":
                if (value instanceof Double) {
                    this.positionSize = (Double) value;
                } else if (value instanceof Integer) {
                    this.positionSize = ((Integer) value).doubleValue();
                }
                break;
            case "stopLossPercent":
                if (value instanceof Double) {
                    this.stopLossPercent = (Double) value;
                } else if (value instanceof Integer) {
                    this.stopLossPercent = ((Integer) value).doubleValue();
                }
                break;
            case "takeProfitPercent":
                if (value instanceof Double) {
                    this.takeProfitPercent = (Double) value;
                } else if (value instanceof Integer) {
                    this.takeProfitPercent = ((Integer) value).doubleValue();
                }
                break;
        }
    }
}
