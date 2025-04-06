package com.tradingbacktester.model;

import java.time.LocalDateTime;

/**
 * Represents a price bar (OHLCV) for a financial instrument at a specific time.
 */
public class Bar {
    private final LocalDateTime timestamp;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final long volume;
    
    /**
     * Creates a new price bar.
     * 
     * @param timestamp the date and time of the bar
     * @param open the opening price
     * @param high the highest price
     * @param low the lowest price
     * @param close the closing price
     * @param volume the trading volume
     */
    public Bar(LocalDateTime timestamp, double open, double high, double low, double close, long volume) {
        this.timestamp = timestamp;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public double getOpen() {
        return open;
    }
    
    public double getHigh() {
        return high;
    }
    
    public double getLow() {
        return low;
    }
    
    public double getClose() {
        return close;
    }
    
    public long getVolume() {
        return volume;
    }
    
    /**
     * Returns the typical price (average of high, low, and close).
     * 
     * @return the typical price
     */
    public double getTypicalPrice() {
        return (high + low + close) / 3.0;
    }
    
    /**
     * Checks whether this bar contains the given price.
     * 
     * @param price the price to check
     * @return true if the price is between low and high (inclusive), false otherwise
     */
    public boolean contains(double price) {
        return price >= low && price <= high;
    }
    
    @Override
    public String toString() {
        return "Bar{" +
                "timestamp=" + timestamp +
                ", open=" + open +
                ", high=" + high +
                ", low=" + low +
                ", close=" + close +
                ", volume=" + volume +
                '}';
    }
}
