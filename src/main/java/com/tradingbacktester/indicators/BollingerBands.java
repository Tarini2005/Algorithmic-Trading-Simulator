package com.tradingbacktester.indicators;

import com.tradingbacktester.model.TimeSeries;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.ArrayList;
import java.util.List;

/**
 * Bollinger Bands indicator.
 */
public class BollingerBands implements Indicator {
    private final int period;
    private final double stdDevMultiplier;
    
    /**
     * Creates a new Bollinger Bands indicator with the default parameters (20, 2.0).
     */
    public BollingerBands() {
        this(20, 2.0);
    }
    
    /**
     * Creates a new Bollinger Bands indicator with the specified parameters.
     * 
     * @param period the period for the moving average
     * @param stdDevMultiplier the standard deviation multiplier
     */
    public BollingerBands(int period, double stdDevMultiplier) {
        if (period <= 1) {
            throw new IllegalArgumentException("Period must be greater than 1");
        }
        if (stdDevMultiplier <= 0) {
            throw new IllegalArgumentException("Standard deviation multiplier must be positive");
        }
        this.period = period;
        this.stdDevMultiplier = stdDevMultiplier;
    }
    
    @Override
    public List<Double> calculate(TimeSeries series) {
        // Calculate the middle band (SMA)
        SMA sma = new SMA(period);
        List<Double> middleBand = sma.calculate(series);
        
        // Return the middle band by default
        return middleBand;
    }
    
    /**
     * Calculates all Bollinger Bands (upper, middle, lower).
     * 
     * @param series the time series
     * @return a list of lists containing the upper band, middle band, and lower band
     */
    public List<List<Double>> calculateBands(TimeSeries series) {
        List<Double> closePrices = series.getClosePrices();
        
        // Calculate the middle band (SMA)
        SMA sma = new SMA(period);
        List<Double> middleBand = sma.calculate(series);
        
        // Calculate the upper and lower bands
        List<Double> upperBand = new ArrayList<>(closePrices.size());
        List<Double> lowerBand = new ArrayList<>(closePrices.size());
        
        for (int i = 0; i < closePrices.size(); i++) {
            if (i < period - 1) {
                upperBand.add(Double.NaN);
                lowerBand.add(Double.NaN);
            } else {
                // Calculate standard deviation
                DescriptiveStatistics stats = new DescriptiveStatistics();
                for (int j = 0; j < period; j++) {
                    stats.addValue(closePrices.get(i - j));
                }
                double stdDev = stats.getStandardDeviation();
                
                // Calculate bands
                double middle = middleBand.get(i);
                upperBand.add(middle + stdDevMultiplier * stdDev);
                lowerBand.add(middle - stdDevMultiplier * stdDev);
            }
        }
        
        List<List<Double>> result = new ArrayList<>(3);
        result.add(upperBand);
        result.add(middleBand);
        result.add(lowerBand);
        
        return result;
    }
    
    /**
     * Calculates the percent B, which indicates where the price is relative to the bands.
     * %B = (Price - Lower Band) / (Upper Band - Lower Band)
     * 
     * @param series the time series
     * @return the percent B values
     */
    public List<Double> calculatePercentB(TimeSeries series) {
        List<Double> closePrices = series.getClosePrices();
        List<List<Double>> bands = calculateBands(series);
        List<Double> upperBand = bands.get(0);
        List<Double> lowerBand = bands.get(2);
        
        List<Double> percentB = new ArrayList<>(closePrices.size());
        
        for (int i = 0; i < closePrices.size(); i++) {
            if (i < period - 1) {
                percentB.add(Double.NaN);
            } else {
                double price = closePrices.get(i);
                double upper = upperBand.get(i);
                double lower = lowerBand.get(i);
                double bandWidth = upper - lower;
                
                if (bandWidth == 0) {
                    percentB.add(0.5); // Middle of the bands
                } else {
                    double pctB = (price - lower) / bandWidth;
                    percentB.add(pctB);
                }
            }
        }
        
        return percentB;
    }
    
    /**
     * Calculates the bandwidth, which indicates the width of the bands relative to the middle band.
     * Bandwidth = (Upper Band - Lower Band) / Middle Band
     * 
     * @param series the time series
     * @return the bandwidth values
     */
    public List<Double> calculateBandwidth(TimeSeries series) {
        List<List<Double>> bands = calculateBands(series);
        List<Double> upperBand = bands.get(0);
        List<Double> middleBand = bands.get(1);
        List<Double> lowerBand = bands.get(2);
        
        List<Double> bandwidth = new ArrayList<>(upperBand.size());
        
        for (int i = 0; i < upperBand.size(); i++) {
            if (i < period - 1) {
                bandwidth.add(Double.NaN);
            } else {
                double upper = upperBand.get(i);
                double middle = middleBand.get(i);
                double lower = lowerBand.get(i);
                
                if (middle == 0) {
                    bandwidth.add(Double.NaN);
                } else {
                    double bw = (upper - lower) / middle;
                    bandwidth.add(bw);
                }
            }
        }
        
        return bandwidth;
    }
    
    @Override
    public String getName() {
        return "BollingerBands(" + period + "," + stdDevMultiplier + ")";
    }
    
    /**
     * Gets the period of the moving average.
     * 
     * @return the period
     */
    public int getPeriod() {
        return period;
    }
    
    /**
     * Gets the standard deviation multiplier.
     * 
     * @return the standard deviation multiplier
     */
    public double getStdDevMultiplier() {
        return stdDevMultiplier;
    }
}
