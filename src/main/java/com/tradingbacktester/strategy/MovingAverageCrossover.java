package com.tradingbacktester.strategy;

import com.tradingbacktester.core.Portfolio;
import com.tradingbacktester.indicators.EMA;
import com.tradingbacktester.indicators.SMA;
import com.tradingbacktester.model.Bar;
import com.tradingbacktester.model.Order;
import com.tradingbacktester.model.OrderType;
import com.tradingbacktester.model.TimeSeries;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Moving Average Crossover strategy.
 * Generates buy signals when the fast moving average crosses above the slow moving average,
 * and sell signals when the fast moving average crosses below the slow moving average.
 */
public class MovingAverageCrossover implements Strategy {
    private final String symbol;
    private int fastPeriod;
    private int slowPeriod;
    private boolean useEMA;
    private double positionSize;
    private double stopLossPercent;
    private double takeProfitPercent;
    
    private Map<String, TimeSeries> data;
    private List<Double> fastMaValues;
    private List<Double> slowMaValues;
    private boolean isPreviousCrossAbove;
    
    /**
     * Creates a new Moving Average Crossover strategy.
     * 
     * @param symbol the symbol to trade
     * @param fastPeriod the period of the fast moving average
     * @param slowPeriod the period of the slow moving average
     * @param useEMA whether to use EMA instead of SMA
     * @param positionSize the position size as a percentage of the portfolio value
     * @param stopLossPercent the stop loss percentage (0 for no stop loss)
     * @param takeProfitPercent the take profit percentage (0 for no take profit)
     */
    public MovingAverageCrossover(String symbol, int fastPeriod, int slowPeriod, boolean useEMA,
                                 double positionSize, double stopLossPercent, double takeProfitPercent) {
        this.symbol = symbol;
        this.fastPeriod = fastPeriod;
        this.slowPeriod = slowPeriod;
        this.useEMA = useEMA;
        this.positionSize = positionSize;
        this.stopLossPercent = stopLossPercent;
        this.takeProfitPercent = takeProfitPercent;
        this.isPreviousCrossAbove = false;
    }
    
    @Override
    public String getName() {
        return "MA Crossover (" + (useEMA ? "EMA" : "SMA") + ", " + fastPeriod + ", " + slowPeriod + ")";
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
        
        // Calculate initial indicator values
        if (useEMA) {
            EMA fastEma = new EMA(fastPeriod);
            EMA slowEma = new EMA(slowPeriod);
            fastMaValues = fastEma.calculate(series);
            slowMaValues = slowEma.calculate(series);
        } else {
            SMA fastSma = new SMA(fastPeriod);
            SMA slowSma = new SMA(slowPeriod);
            fastMaValues = fastSma.calculate(series);
            slowMaValues = slowSma.calculate(series);
        }
        
        // Initialize cross state
        int lastValidIndex = findLastValidIndex();
        if (lastValidIndex >= 0) {
            isPreviousCrossAbove = fastMaValues.get(lastValidIndex) > slowMaValues.get(lastValidIndex);
        }
    }
    
    /**
     * Finds the last index where both moving averages have valid values.
     * 
     * @return the last valid index, or -1 if none
     */
    private int findLastValidIndex() {
        for (int i = fastMaValues.size() - 1; i >= 0; i--) {
            if (!Double.isNaN(fastMaValues.get(i)) && !Double.isNaN(slowMaValues.get(i))) {
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
        
        // Update indicator values
        if (useEMA) {
            EMA fastEma = new EMA(fastPeriod);
            EMA slowEma = new EMA(slowPeriod);
            fastMaValues = fastEma.calculate(series);
            slowMaValues = slowEma.calculate(series);
        } else {
            SMA fastSma = new SMA(fastPeriod);
            SMA slowSma = new SMA(slowPeriod);
            fastMaValues = fastSma.calculate(series);
            slowMaValues = slowSma.calculate(series);
        }
    }
    
    @Override
    public List<Order> generateOrders(LocalDateTime timestamp, Map<String, Bar> currentBars, Portfolio portfolio) {
        Bar bar = currentBars.get(symbol);
        if (bar == null) {
            return Collections.emptyList();
        }
        
        int lastIndex = fastMaValues.size() - 1;
        if (lastIndex < 0 || Double.isNaN(fastMaValues.get(lastIndex)) || Double.isNaN(slowMaValues.get(lastIndex))) {
            return Collections.emptyList();
        }
        
        double fastMa = fastMaValues.get(lastIndex);
        double slowMa = slowMaValues.get(lastIndex);
        boolean isCrossAbove = fastMa > slowMa;
        
        List<Order> orders = new ArrayList<>();
        
        // Check for crossover
        if (isCrossAbove != isPreviousCrossAbove) {
            // Crossover occurred
            if (isCrossAbove) {
                // Bullish crossover (fast MA crosses above slow MA)
                if (portfolio.hasPosition(symbol) && portfolio.getPosition(symbol).isShort()) {
                    // Close short position
                    orders.add(new Order(symbol, OrderType.MARKET, -portfolio.getPosition(symbol).getQuantity(), timestamp));
                }
                
                // Open long position
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
            } else {
                // Bearish crossover (fast MA crosses below slow MA)
                if (portfolio.hasPosition(symbol) && portfolio.getPosition(symbol).isLong()) {
                    // Close long position
                    orders.add(new Order(symbol, OrderType.MARKET, -portfolio.getPosition(symbol).getQuantity(), timestamp));
                }
                
                // Open short position if allowed
                // Note: Uncommenting the following code would enable short selling
                /*
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
                */
            }
            
            isPreviousCrossAbove = isCrossAbove;
        }
        
        return orders;
    }
    
    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("fastPeriod", fastPeriod);
        params.put("slowPeriod", slowPeriod);
        params.put("useEMA", useEMA);
        params.put("positionSize", positionSize);
        params.put("stopLossPercent", stopLossPercent);
        params.put("takeProfitPercent", takeProfitPercent);
        return params;
    }
    
    @Override
    public void setParameter(String name, Object value) {
        switch (name) {
            case "fastPeriod":
                if (value instanceof Integer) {
                    this.fastPeriod = (Integer) value;
                }
                break;
            case "slowPeriod":
                if (value instanceof Integer) {
                    this.slowPeriod = (Integer) value;
                }
                break;
            case "useEMA":
                if (value instanceof Boolean) {
                    this.useEMA = (Boolean) value;
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
