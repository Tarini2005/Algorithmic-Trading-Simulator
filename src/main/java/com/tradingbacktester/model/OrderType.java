package com.tradingbacktester.model;

/**
 * Enumeration of different order types.
 */
public enum OrderType {
    /**
     * Market order - executed immediately at the current market price.
     */
    MARKET("Market"),
    
    /**
     * Limit order - executed only when the price reaches a specific level or better.
     */
    LIMIT("Limit"),
    
    /**
     * Stop order - becomes a market order when the price reaches a specific level.
     */
    STOP("Stop"),
    
    /**
     * Stop-limit order - becomes a limit order when the price reaches a specific level.
     */
    STOP_LIMIT("Stop-Limit");
    
    private final String displayName;
    
    OrderType(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * Gets the display name of the order type.
     * 
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
