package com.tradingbacktester.indicators;

import com.tradingbacktester.model.TimeSeries;
import java.util.ArrayList;
import java.util.List;

/**
 * Moving Average Convergence Divergence (MACD) indicator.
 */
public class MACD implements Indicator {
    private final int fastPeriod;
    private final int slowPeriod;
    private final int signalPeriod;
    
    /**
     * Creates a new MACD indicator with the default parameters (12, 26, 9).
     */
    public MACD() {
        this(12, 26, 9);
    }
    
    /**
     * Creates a new MACD indicator with the specified parameters.
     * 
     * @param fastPeriod the period for the fast EMA
     * @param slowPeriod the period for the slow EMA
     * @param signalPeriod the period for the signal line
     */
    public MACD(int fastPeriod, int slowPeriod, int signalPeriod) {
        if (fastPeriod <= 0 || slowPeriod <= 0 || signalPeriod <= 0) {
            throw new IllegalArgumentException("Periods must be positive");
        }
        if (fastPeriod >= slowPeriod) {
            throw new IllegalArgumentException("Fast period must be less than slow period");
        }
        this.fastPeriod = fastPeriod;
        this.slowPeriod = slowPeriod;
        this.signalPeriod = signalPeriod;
    }
    
    @Override
    public List<Double> calculate(TimeSeries series) {
        // Calculate fast and slow EMAs
        EMA fastEma = new EMA(fastPeriod);
        EMA slowEma = new EMA(slowPeriod);
        
        List<Double> fastValues = fastEma.calculate(series);
        List<Double> slowValues = slowEma.calculate(series);
        
        // Calculate MACD line (fast EMA - slow EMA)
        List<Double> macdLine = new ArrayList<>(fastValues.size());
        for (int i = 0; i < fastValues.size(); i++) {
            if (i < slowPeriod - 1) {
                macdLine.add(Double.NaN);
            } else {
                macdLine.add(fastValues.get(i) - slowValues.get(i));
            }
        }
        
        // Calculate signal line (EMA of MACD line)
        List<Double> signalLine = calculateSignalLine(macdLine);
        
        // Calculate histogram (MACD line - signal line)
        List<Double> histogram = new ArrayList<>(macdLine.size());
        for (int i = 0; i < macdLine.size(); i++) {
            if (i < slowPeriod + signalPeriod - 2) {
                histogram.add(Double.NaN);
            } else {
                histogram.add(macdLine.get(i) - signalLine.get(i));
            }
