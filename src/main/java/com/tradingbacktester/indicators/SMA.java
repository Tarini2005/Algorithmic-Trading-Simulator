package com.tradingbacktester.indicators;

import com.tradingbacktester.model.TimeSeries;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple Moving Average (SMA) indicator.
 */
public class SMA implements Indicator {
    private final int period;
    
    /**
     * Creates a new SMA indicator with the specified period.
     * 
     * @param period the period of the moving average
     */
    public SMA(int period) {
        if (period <= 0) {
            throw new IllegalArgumentException("Period must be positive");
        }
        this.period = period;
    }
    
    @Override
    public List<Double> calculate(TimeSeries series) {
        List<Double> closePrices = series.getClosePrices();
        List<Double> smaValues = new ArrayList<>(closePrices.size());
        
        // Fill with NaN until we have enough data points
        for (int i = 0; i < period - 1; i++) {
            smaValues.add(Double.NaN);
        }
        
        for (int i = period - 1; i < closePrices.size(); i++) {
            double sum = 0;
            for (int j = 0; j < period; j++) {
                sum += closePrices.get(i - j);
            }
            smaValues.add(sum / period);
        }
        
        return smaValues;
    }
    
    @Override
    public String getName() {
        return "SMA(" + period + ")";
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
