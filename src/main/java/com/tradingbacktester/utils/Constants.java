package com.tradingbacktester.utils;

/**
 * Constants used throughout the application.
 */
public final class Constants {
    
    // Default commission rate (0.1%)
    public static final double DEFAULT_COMMISSION_RATE = 0.001;
    
    // Default slippage model (0.1%)
    public static final double DEFAULT_SLIPPAGE = 0.001;
    
    // Default date/time format
    public static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    
    // Default chart colors
    public static final String PRICE_COLOR = "#1f77b4";
    public static final String VOLUME_COLOR = "#7f7f7f";
    public static final String PROFIT_COLOR = "#2ca02c";
    public static final String LOSS_COLOR = "#d62728";
    public static final String MA_FAST_COLOR = "#ff7f0e";
    public static final String MA_SLOW_COLOR = "#9467bd";
    public static final String UPPER_BAND_COLOR = "#aec7e8";
    public static final String MIDDLE_BAND_COLOR = "#1f77b4";
    public static final String LOWER_BAND_COLOR = "#aec7e8";
    public static final String RSI_COLOR = "#ff7f0e";
    public static final String MACD_LINE_COLOR = "#1f77b4";
    public static final String SIGNAL_LINE_COLOR = "#ff7f0e";
    public static final String HISTOGRAM_POSITIVE_COLOR = "#2ca02c";
    public static final String HISTOGRAM_NEGATIVE_COLOR = "#d62728";
    
    // Levels
    public static final double RSI_OVERBOUGHT_LEVEL = 70.0;
    public static final double RSI_OVERSOLD_LEVEL = 30.0;
    
    // Strategy default parameters
    public static final int MA_FAST_PERIOD_DEFAULT = 12;
    public static final int MA_SLOW_PERIOD_DEFAULT = 26;
    public static final int RSI_PERIOD_DEFAULT = 14;
    public static final double POSITION_SIZE_DEFAULT = 0.1; // 10% of portfolio
    
    // Prevent instantiation
    private Constants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
