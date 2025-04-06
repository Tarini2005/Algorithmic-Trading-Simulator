package com.tradingbacktester.indicators;

import com.tradingbacktester.model.TimeSeries;
import java.util.ArrayList;
import java.util.List;

/**
 * Relative Strength Index (RSI) indicator.
 */
public class RSI implements Indicator {
    private final int period;
    
    /**
     * Creates a new RSI indicator with the specified period.
     * 
     * @param period the period for the RSI calculation
     */
    public RSI(int period) {
        if (period <= 1) {
            throw new IllegalArgumentException("Period must be greater than 1");
        }
        this.period = period;
    }
    
    @Override
    public List<Double> calculate(TimeSeries series) {
        List<Double> closePrices = series.getClosePrices();
        List<Double> rsiValues = new ArrayList<>(closePrices.size());
        
        if (closePrices.size() <= period) {
            // Not enough data points
            for (int i = 0; i < closePrices.size(); i++) {
                rsiValues.add(Double.NaN);
            }
            return rsiValues;
        }
        
        // Fill with NaN until we have enough data points for the first RS calculation
        for (int i = 0; i < period; i++) {
            rsiValues.add(Double.NaN);
        }
        
        // Calculate price changes
        List<Double> priceChanges = new ArrayList<>(closePrices.size() - 1);
        for (int i = 1; i < closePrices.size(); i++) {
            priceChanges.add(closePrices.get(i) - closePrices.get(i - 1));
        }
        
        // Calculate initial averages
        double initialGainSum = 0;
        double initialLossSum = 0;
        
        for (int i = 0; i < period; i++) {
            double change = priceChanges.get(i);
            if (change > 0) {
                initialGainSum += change;
            } else {
                initialLossSum += Math.abs(change);
            }
        }
        
        double avgGain = initialGainSum / period;
        double avgLoss = initialLossSum / period;
        
        // Calculate first RSI
        double rs = avgLoss == 0 ? 100 : avgGain / avgLoss;
        double rsi = 100 - (100 / (1 + rs));
        rsiValues.add(rsi);
        
        // Calculate the rest of the RSI values
        for (int i = period; i < priceChanges.size(); i++) {
            double change = priceChanges.get(i);
            double gain = change > 0 ? change : 0;
            double loss = change < 0 ? Math.abs(change) : 0;
            
            // Smooth averages
            avgGain = ((avgGain * (period - 1)) + gain) / period;
            avgLoss = ((avgLoss * (period - 1)) + loss) / period;
            
            // Calculate RSI
            rs = avgLoss == 0 ? 100 : avgGain / avgLoss;
            rsi = 100 - (100 / (1 + rs));
            rsiValues.add(rsi);
        }
        
        return rsiValues;
    }
    
    @Override
    public String getName() {
        return "RSI(" + period + ")";
    }
    
    /**
     * Gets the period of the RSI.
     * 
     * @return the period
     */
    public int getPeriod() {
        return period;
    }
}
