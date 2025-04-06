package com.tradingbacktester.utils;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.List;

/**
 * Mathematical utility methods.
 */
public final class MathUtils {
    
    /**
     * Calculates the standard deviation of a series of values.
     * 
     * @param values the values
     * @return the standard deviation
     */
    public static double standardDeviation(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return Double.NaN;
        }
        
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (Double value : values) {
            if (!Double.isNaN(value)) {
                stats.addValue(value);
            }
        }
        
        return stats.getStandardDeviation();
    }
    
    /**
     * Calculates the mean of a series of values.
     * 
     * @param values the values
     * @return the mean
     */
    public static double mean(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return Double.NaN;
        }
        
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (Double value : values) {
            if (!Double.isNaN(value)) {
                stats.addValue(value);
            }
        }
        
        return stats.getMean();
    }
    
    /**
     * Calculates the minimum of a series of values.
     * 
     * @param values the values
     * @return the minimum
     */
    public static double min(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return Double.NaN;
        }
        
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (Double value : values) {
            if (!Double.isNaN(value)) {
                stats.addValue(value);
            }
        }
        
        return stats.getMin();
    }
    
    /**
     * Calculates the maximum of a series of values.
     * 
     * @param values the values
     * @return the maximum
     */
    public static double max(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return Double.NaN;
        }
        
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (Double value : values) {
            if (!Double.isNaN(value)) {
                stats.addValue(value);
            }
        }
        
        return stats.getMax();
    }
    
    /**
     * Calculates the coefficient of variation of a series of values.
     * 
     * @param values the values
     * @return the coefficient of variation
     */
    public static double coefficientOfVariation(List<Double> values) {
        double mean = mean(values);
        double stdDev = standardDeviation(values);
        
        if (mean == 0 || Double.isNaN(mean) || Double.isNaN(stdDev)) {
            return Double.NaN;
        }
        
        return stdDev / mean;
    }
    
    /**
     * Calculates the annualized return from a series of period returns.
     * 
     * @param periodReturns the period returns (as decimals, e.g., 0.05 for 5%)
     * @param periodsPerYear the number of periods in a year
     * @return the annualized return
     */
    public static double annualizedReturn(List<Double> periodReturns, int periodsPerYear) {
        if (periodReturns == null || periodReturns.isEmpty()) {
            return Double.NaN;
        }
        
        double cumulativeReturn = 1.0;
        int validPeriods = 0;
        
        for (Double periodReturn : periodReturns) {
            if (!Double.isNaN(periodReturn)) {
                cumulativeReturn *= (1 + periodReturn);
                validPeriods++;
            }
        }
        
        if (validPeriods == 0) {
            return Double.NaN;
        }
        
        double periodsInSample = validPeriods;
        return Math.pow(cumulativeReturn, periodsPerYear / periodsInSample) - 1;
    }
    
    /**
     * Calculates the Sharpe ratio.
     * 
     * @param periodReturns the period returns (as decimals, e.g., 0.05 for 5%)
     * @param riskFreeRate the risk-free rate (as a decimal, e.g., 0.02 for 2%)
     * @param periodsPerYear the number of periods in a year
     * @return the Sharpe ratio
     */
    public static double sharpeRatio(List<Double> periodReturns, double riskFreeRate, int periodsPerYear) {
        if (periodReturns == null || periodReturns.isEmpty()) {
            return Double.NaN;
        }
        
        // Convert annual risk-free rate to period rate
        double periodRiskFreeRate = Math.pow(1 + riskFreeRate, 1.0 / periodsPerYear) - 1;
        
        // Calculate excess returns
        double[] excessReturns = new double[periodReturns.size()];
        int validPeriods = 0;
        
        for (int i = 0; i < periodReturns.size(); i++) {
            if (!Double.isNaN(periodReturns.get(i))) {
                excessReturns[validPeriods++] = periodReturns.get(i) - periodRiskFreeRate;
            }
        }
        
        if (validPeriods == 0) {
            return Double.NaN;
        }
        
        // Calculate mean and standard deviation of excess returns
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (int i = 0; i < validPeriods; i++) {
            stats.addValue(excessReturns[i]);
        }
        
        double meanExcessReturn = stats.getMean();
        double stdDevExcessReturn = stats.getStandardDeviation();
        
        if (stdDevExcessReturn == 0 || Double.isNaN(stdDevExcessReturn)) {
            return Double.NaN;
        }
        
        // Calculate and return annualized Sharpe ratio
        return (meanExcessReturn / stdDevExcessReturn) * Math.sqrt(periodsPerYear);
    }
    
    /**
     * Calculates the maximum drawdown of a series of values.
     * 
     * @param values the values
     * @return the maximum drawdown as a positive percentage
     */
    public static double maxDrawdown(List<Double> values) {
        if (values == null || values.size() < 2) {
            return 0.0;
        }
        
        double maxSoFar = values.get(0);
        double maxDrawdown = 0.0;
        
        for (int i = 1; i < values.size(); i++) {
            double value = values.get(i);
            if (!Double.isNaN(value)) {
                double drawdown = (maxSoFar - value) / maxSoFar;
                maxDrawdown = Math.max(maxDrawdown, drawdown);
                maxSoFar = Math.max(maxSoFar, value);
            }
        }
        
        return maxDrawdown;
    }
    
    /**
     * Calculates the compounded annual growth rate (CAGR).
     * 
     * @param startValue the starting value
     * @param endValue the ending value
     * @param years the number of years
     * @return the CAGR
     */
    public static double cagr(double startValue, double endValue, double years) {
        if (startValue <= 0 || endValue <= 0 || years <= 0) {
            return Double.NaN;
        }
        
        return Math.pow(endValue / startValue, 1.0 / years) - 1;
    }
    
    // Prevent instantiation
    private MathUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
