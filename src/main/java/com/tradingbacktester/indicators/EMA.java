package com.tradingbacktester.indicators;

import com.tradingbacktester.model.TimeSeries;
import java.util.ArrayList;
import java.util.List;

/**
 * Exponential Moving Average (EMA) indicator.
 */
public class EMA implements Indicator {
    private final int period;
    
    /**
     * Creates a new EMA indicator with the specified period.
     * 
     * @param period the period of the moving average
     */
    public EMA(int period) {
        if (period <= 0) {
            throw new IllegalArgumentException("Period must be positive");
        }
        this.period = period;
    }
    
    @Override
    public List<Double> calculate(TimeSeries series) {
        List<Double> closePrices = series.getClosePrices();
        List<Double> emaValues = new ArrayList<>(closePrices.size());
        
        if (closePrices.isEmpty()) {
            return emaValues;
        }
        
        // Calculate multiplier
        double multiplier = 2.0 / (period + 1);
        
        // Calculate SMA for the first value
        double sum = 0;
        for (int i = 0; i < Math.min(period, closePrices.size()); i++) {
            sum += closePrices.get(i);
        }
        double sma = sum / Math.min(period, closePrices.size());
        
        // Fill with NaN until we have enough data points, then start with the SMA
        for (int i = 0; i < period - 1; i++) {
            emaValues.add(Double.NaN);
        }
        emaValues.add(sma);
        
        // Calculate EMA for the rest
        for (int i = period; i < closePrices.size(); i++) {
            double price = closePrices.get(i);
            double previousEma = emaValues.get(i - 1);
            double ema = (price - previousEma) * multiplier + previousEma;
            emaValues.add(ema);
        }
        
        return emaValues;
    }
    
    @Override
    public String getName() {
        return "EMA(" + period + ")";
    }
    
    /**
     * Gets the period of the moving average.
     * 
     * @return the period
     */
    public int getPeriod() {
        return period;
    }
}
