package com.tradingbacktester.risk;

import com.tradingbacktester.model.Bar;
import com.tradingbacktester.model.TimeSeries;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculates stop loss levels based on different methods.
 */
public class StopLossCalculator {
    
    /**
     * Calculates a fixed percentage stop loss.
     * 
     * @param entryPrice the entry price
     * @param percentage the stop loss percentage
     * @param isLong true for long position, false for short position
     * @return the stop loss price
     */
    public static double calculatePercentageStop(double entryPrice, double percentage, boolean isLong) {
        if (percentage <= 0) {
            throw new IllegalArgumentException("Percentage must be positive");
        }
        
        if (isLong) {
            return entryPrice * (1 - percentage / 100.0);
        } else {
            return entryPrice * (1 + percentage / 100.0);
        }
    }
    
    /**
     * Calculates a fixed amount stop loss.
     * 
     * @param entryPrice the entry price
     * @param amount the stop loss amount
     * @param isLong true for long position, false for short position
     * @return the stop loss price
     */
    public static double calculateFixedAmountStop(double entryPrice, double amount, boolean isLong) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        if (isLong) {
            return entryPrice - amount;
        } else {
            return entryPrice + amount;
        }
    }
    
    /**
     * Calculates the Average True Range (ATR) for a time series.
     * 
     * @param series the time series
     * @param period the period for the ATR calculation
     * @return the ATR values
     */
    public static List<Double> calculateATR(TimeSeries series, int period) {
        List<Bar> bars = series.getBars();
        List<Double> trueRanges = new ArrayList<>();
        List<Double> atrValues = new ArrayList<>();
        
        // Calculate true ranges
        for (int i = 0; i < bars.size(); i++) {
            Bar bar = bars.get(i);
            double highLowRange = bar.getHigh() - bar.getLow();
            
            double trueRange;
            if (i == 0) {
                trueRange = highLowRange;
            } else {
                Bar prevBar = bars.get(i - 1);
                double highCloseDiff = Math.abs(bar.getHigh() - prevBar.getClose());
                double lowCloseDiff = Math.abs(bar.getLow() - prevBar.getClose());
                trueRange = Math.max(highLowRange, Math.max(highCloseDiff, lowCloseDiff));
            }
            
            trueRanges.add(trueRange);
        }
        
        // Calculate ATR (simple moving average of true ranges for now)
        for (int i = 0; i < trueRanges.size(); i++) {
            if (i < period - 1) {
                atrValues.add(Double.NaN);
            } else {
                double sum = 0;
                for (int j = 0; j < period; j++) {
                    sum += trueRanges.get(i - j);
                }
                atrValues.add(sum / period);
            }
        }
        
        return atrValues;
    }
    
    /**
     * Calculates a Volatility-based stop loss using ATR.
     * 
     * @param series the time series
     * @param period the period for the ATR calculation
     * @param multiplier the ATR multiplier
     * @param entryPrice the entry price
     * @param isLong true for long position, false for short position
     * @return the stop loss price
     */
    public static double calculateAtrStop(TimeSeries series, int period, double multiplier, double entryPrice, boolean isLong) {
        List<Double> atrValues = calculateATR(series, period);
        double atr = atrValues.get(atrValues.size() - 1);
        
        if (isLong) {
            return entryPrice - (atr * multiplier);
        } else {
            return entryPrice + (atr * multiplier);
        }
    }
    
    /**
     * Calculates a Chandelier Exit stop loss.
     * 
     * @param series the time series
     * @param period the lookback period
     * @param atrMultiplier the ATR multiplier
     * @param isLong true for long position, false for short position
     * @return the stop loss price
     */
    public static double calculateChandelierExit(TimeSeries series, int period, double atrMultiplier, boolean isLong) {
        List<Bar> bars = series.getBars();
        
        // We need at least 'period' bars
        if (bars.size() < period) {
            throw new IllegalArgumentException("Not enough bars for calculation");
        }
        
        // Calculate highest high and lowest low
        double highestHigh = Double.NEGATIVE_INFINITY;
        double lowestLow = Double.POSITIVE_INFINITY;
        
        for (int i = bars.size() - period; i < bars.size(); i++) {
            Bar bar = bars.get(i);
            highestHigh = Math.max(highestHigh, bar.getHigh());
            lowestLow = Math.min(lowestLow, bar.getLow());
        }
        
        // Calculate ATR
        List<Double> atrValues = calculateATR(series, period);
        double atr = atrValues.get(atrValues.size() - 1);
        
        // Calculate Chandelier Exit
        if (isLong) {
            return highestHigh - (atr * atrMultiplier);
        } else {
            return lowestLow + (atr * atrMultiplier);
        }
    }
    
    /**
     * Calculates a stop loss based on Bollinger Bands.
     * 
     * @param series the time series
     * @param period the period for the Bollinger Bands calculation
     * @param stdDevMultiplier the standard deviation multiplier
     * @param isLong true for long position, false for short position
     * @return the stop loss price
     */
    public static double calculateBollingerBandStop(TimeSeries series, int period, double stdDevMultiplier, boolean isLong) {
        List<Bar> bars = series.getBars();
        
        // We need at least 'period' bars
        if (bars.size() < period) {
            throw new IllegalArgumentException("Not enough bars for calculation");
        }
        
        // Calculate SMA
        double sum = 0;
        for (int i = bars.size() - period; i < bars.size(); i++) {
            sum += bars.get(i).getClose();
        }
        double sma = sum / period;
        
        // Calculate standard deviation
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (int i = bars.size() - period; i < bars.size(); i++) {
            stats.addValue(bars.get(i).getClose());
        }
        double stdDev = stats.getStandardDeviation();
        
        // Calculate Bollinger Bands
        double upperBand = sma + (stdDev * stdDevMultiplier);
        double lowerBand = sma - (stdDev * stdDevMultiplier);
        
        // Return appropriate band based on position type
        return isLong ? lowerBand : upperBand;
    }
}
