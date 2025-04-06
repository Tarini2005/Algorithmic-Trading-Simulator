package com.tradingbacktester.indicators;

import com.tradingbacktester.model.TimeSeries;
import java.util.List;

/**
 * Interface for technical indicators.
 */
public interface Indicator {
    
    /**
     * Calculates the indicator values for the given time series.
     * 
     * @param series the time series
     * @return a list of indicator values
     */
    List<Double> calculate(TimeSeries series);
    
    /**
     * Gets the name of the indicator.
     * 
     * @return the indicator name
     */
    String getName();
    
    /**
     * Gets the most recent value of the indicator.
     * 
     * @param series the time series
     * @return the most recent value
     */
    default double getValue(TimeSeries series) {
        List<Double> values = calculate(series);
        return values.isEmpty() ? Double.NaN : values.get(values.size() - 1);
    }
}
