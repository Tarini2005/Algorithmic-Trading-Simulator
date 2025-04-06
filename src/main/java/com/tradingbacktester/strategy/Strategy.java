package com.tradingbacktester.strategy;

import com.tradingbacktester.core.Portfolio;
import com.tradingbacktester.model.Bar;
import com.tradingbacktester.model.Order;
import com.tradingbacktester.model.TimeSeries;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface for trading strategies.
 */
public interface Strategy {
    
    /**
     * Gets the name of the strategy.
     * 
     * @return the strategy name
     */
    String getName();
    
    /**
     * Gets the set of symbols required by this strategy.
     * 
     * @return the set of required symbols
     */
    Set<String> getRequiredSymbols();
    
    /**
     * Initializes the strategy with the market data.
     * 
     * @param data a map of symbol to time series
     */
    void initialize(Map<String, TimeSeries> data);
    
    /**
     * Called when a new price bar is received.
     * 
     * @param timestamp the timestamp of the new bar
     * @param currentBars the current price bars for all symbols
     * @param portfolio the current portfolio
     */
    void onBar(LocalDateTime timestamp, Map<String, Bar> currentBars, Portfolio portfolio);
    
    /**
     * Generates orders based on the current market state.
     * 
     * @param timestamp the current timestamp
     * @param currentBars the current price bars for all symbols
     * @param portfolio the current portfolio
     * @return a list of orders to execute
     */
    List<Order> generateOrders(LocalDateTime timestamp, Map<String, Bar> currentBars, Portfolio portfolio);
    
    /**
     * Gets the parameters of the strategy.
     * 
     * @return a map of parameter name to value
     */
    Map<String, Object> getParameters();
    
    /**
     * Sets a parameter of the strategy.
     * 
     * @param name the parameter name
     * @param value the parameter value
     */
    void setParameter(String name, Object value);
}
