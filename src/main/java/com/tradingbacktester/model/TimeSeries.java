package com.tradingbacktester.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Represents a time series of price bars for a financial instrument.
 */
public class TimeSeries {
    private final String symbol;
    private final NavigableMap<LocalDateTime, Bar> bars;
    private final List<LocalDateTime> timestamps;
    
    /**
     * Creates a new empty time series.
     * 
     * @param symbol the symbol of the financial instrument
     */
    public TimeSeries(String symbol) {
        this.symbol = symbol;
        this.bars = new TreeMap<>();
        this.timestamps = new ArrayList<>();
    }
    
    /**
     * Creates a new time series with the given bars.
     * 
     * @param symbol the symbol of the financial instrument
     * @param initialBars the initial bars to add
     */
    public TimeSeries(String symbol, List<Bar> initialBars) {
        this(symbol);
        for (Bar bar : initialBars) {
            addBar(bar);
        }
    }
    
    /**
     * Adds a bar to the time series.
     * 
     * @param bar the bar to add
     */
    public void addBar(Bar bar) {
        bars.put(bar.getTimestamp(), bar);
        timestamps.add(bar.getTimestamp());
    }
    
    /**
     * Gets the bar at the specified index.
     * 
     * @param index the index of the bar
     * @return the bar at the specified index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public Bar getBar(int index) {
        return bars.get(timestamps.get(index));
    }
    
    /**
     * Gets the bar at the specified timestamp.
     * 
     * @param timestamp the timestamp of the bar
     * @return the bar at the specified timestamp, or null if not found
     */
    public Bar getBar(LocalDateTime timestamp) {
        return bars.get(timestamp);
    }
    
    /**
     * Gets the closing prices of all bars in the time series.
     * 
     * @return a list of closing prices
     */
    public List<Double> getClosePrices() {
        List<Double> closePrices = new ArrayList<>(bars.size());
        for (LocalDateTime timestamp : timestamps) {
            closePrices.add(bars.get(timestamp).getClose());
        }
        return closePrices;
    }
    
    /**
     * Gets the closing prices for a specified number of bars back from the end.
     * 
     * @param period the number of bars to retrieve
     * @return a list of closing prices
     */
    public List<Double> getClosePrices(int period) {
        int size = timestamps.size();
        int startIndex = Math.max(0, size - period);
        List<Double> closePrices = new ArrayList<>(size - startIndex);
        
        for (int i = startIndex; i < size; i++) {
            closePrices.add(bars.get(timestamps.get(i)).getClose());
        }
        
        return closePrices;
    }
    
    /**
     * Gets all bars in the time series.
     * 
     * @return an unmodifiable list of all bars
     */
    public List<Bar> getBars() {
        List<Bar> barList = new ArrayList<>(bars.size());
        for (LocalDateTime timestamp : timestamps) {
            barList.add(bars.get(timestamp));
        }
        return Collections.unmodifiableList(barList);
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
     * Gets the number of bars in the time series.
     * 
     * @return the number of bars
     */
    public int getBarCount() {
        return bars.size();
    }
    
    /**
     * Gets the first bar in the time series.
     * 
     * @return the first bar, or null if the time series is empty
     */
    public Bar getFirstBar() {
        if (timestamps.isEmpty()) {
            return null;
        }
        return bars.get(timestamps.get(0));
    }
    
    /**
     * Gets the last bar in the time series.
     * 
     * @return the last bar, or null if the time series is empty
     */
    public Bar getLastBar() {
        if (timestamps.isEmpty()) {
            return null;
        }
        return bars.get(timestamps.get(timestamps.size() - 1));
    }
    
    /**
     * Gets a sub-series of this time series from startIndex to endIndex (inclusive).
     * 
     * @param startIndex the start index (inclusive)
     * @param endIndex the end index (inclusive)
     * @return a new time series containing the specified range of bars
     * @throws IndexOutOfBoundsException if the indices are out of range
     */
    public TimeSeries getSubSeries(int startIndex, int endIndex) {
        if (startIndex < 0 || endIndex >= timestamps.size() || startIndex > endIndex) {
            throw new IndexOutOfBoundsException("Invalid index range: " + startIndex + " to " + endIndex);
        }
        
        List<Bar> subBars = new ArrayList<>(endIndex - startIndex + 1);
        for (int i = startIndex; i <= endIndex; i++) {
            subBars.add(bars.get(timestamps.get(i)));
        }
        
        return new TimeSeries(symbol, subBars);
    }
}
