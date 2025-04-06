package com.tradingbacktester.indicators;

import com.tradingbacktester.model.Bar;
import com.tradingbacktester.model.TimeSeries;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the technical indicators.
 */
public class IndicatorsTest {
    
    private TimeSeries series;
    
    @BeforeEach
    public void setUp() {
        series = new TimeSeries("TEST");
        
        // Create sample price data
        LocalDateTime time = LocalDateTime.of(2022, 1, 1, 0, 0);
        double[] prices = {
            100, 102, 104, 103, 105, 107, 109, 108, 106, 105,
            107, 109, 110, 112, 111, 113, 114, 116, 118, 117,
            115, 113, 114, 116, 118, 120, 119, 117, 115, 118
        };
        
        for (int i = 0; i < prices.length; i++) {
            double price = prices[i];
            double open = price - 0.5;
            double high = price + 1.0;
            double low = price - 1.0;
            double close = price;
            
            Bar bar = new Bar(time.plusDays(i), open, high, low, close, 100000);
            series.addBar(bar);
        }
    }
    
    @Test
    public void testSMA() {
        // Calculate 5-period SMA
        SMA sma = new SMA(5);
        List<Double> values = sma.calculate(series);
        
        // Verify values
        assertEquals(series.getBarCount(), values.size());
        
        // First 4 values should be NaN
        for (int i = 0; i < 4; i++) {
            assertTrue(Double.isNaN(values.get(i)));
        }
        
        // 5th value should be average of first 5 closes
        double expected = (100 + 102 + 104 + 103 + 105) / 5.0;
        assertEquals(expected, values.get(4), 0.001);
        
        // Test consecutive values
        expected = (102 + 104 + 103 + 105 + 107) / 5.0;
        assertEquals(expected, values.get(5), 0.001);
        
        // Test last value
        int lastIndex = values.size() - 1;
        expected = (115 + 117 + 115 + 118 + 118) / 5.0;
        assertEquals(expected, values.get(lastIndex), 0.001);
    }
    
    @Test
    public void testEMA() {
        // Calculate 5-period EMA
        EMA ema = new EMA(5);
        List<Double> values = ema.calculate(series);
        
        // Verify values
        assertEquals(series.getBarCount(), values.size());
        
        // First 4 values should be NaN
        for (int i = 0; i < 4; i++) {
            assertTrue(Double.isNaN(values.get(i)));
        }
        
        // 5th value should be simple average of first 5 closes (SMA as starting point)
        double expected = (100 + 102 + 104 + 103 + 105) / 5.0;
        assertEquals(expected, values.get(4), 0.001);
        
        // Test EMA formula for 6th value
        double multiplier = 2.0 / (5 + 1);
        expected = (107 - expected) * multiplier + expected;
        assertEquals(expected, values.get(5), 0.001);
    }
    
    @Test
    public void testRSI() {
        // Calculate 14-period RSI
        RSI rsi = new RSI(14);
        List<Double> values = rsi.calculate(series);
        
        // Verify values
        assertEquals(series.getBarCount(), values.size());
        
        // Values in valid range
        for (Double value : values) {
            if (!Double.isNaN(value)) {
                assertTrue(value >= 0 && value <= 100);
            }
        }
        
        // First 14 values should be NaN
        for (int i = 0; i < 14; i++) {
            assertTrue(Double.isNaN(values.get(i)));
        }
        
        // Test formula for sample values
        // (Not testing exact values because RSI calculation is complex,
        // but verifying it's in a reasonable range)
        double rsiValue = values.get(20);
        assertTrue(rsiValue > 40 && rsiValue < 60);
    }
    
    @Test
    public void testMACD() {
        // Calculate MACD with default parameters (12, 26, 9)
        MACD macd = new MACD();
        List<Double> histogram = macd.calculate(series);
        
        // Verify values
        assertEquals(series.getBarCount(), histogram.size());
        
        // First several values should be NaN
        for (int i = 0; i < 26; i++) {
            assertTrue(Double.isNaN(histogram.get(i)));
        }
        
        // Get MACD components
        List<List<Double>> lines = macd.calculateLines(series);
        List<Double> macdLine = lines.get(0);
        List<Double> signalLine = lines.get(1);
        List<Double> histogramValues = lines.get(2);
        
        // Verify line lengths
        assertEquals(series.getBarCount(), macdLine.size());
        assertEquals(series.getBarCount(), signalLine.size());
        assertEquals(series.getBarCount(), histogramValues.size());
        
        // Verify histogram calculation
        for (int i = 0; i < series.getBarCount(); i++) {
            if (!Double.isNaN(macdLine.get(i)) && !Double.isNaN(signalLine.get(i))) {
                double expected = macdLine.get(i) - signalLine.get(i);
                assertEquals(expected, histogramValues.get(i), 0.001);
                assertEquals(expected, histogram.get(i), a0.001);
            }
        }
    }
    
    @Test
    public void testBollingerBands() {
        // Calculate Bollinger Bands with default parameters (20, 2.0)
        BollingerBands bb = new BollingerBands();
        List<Double> middleBand = bb.calculate(series);
        
        // Verify middle band values (should be same as 20-period SMA)
        assertEquals(series.getBarCount(), middleBand.size());
        
        SMA sma = new SMA(20);
        List<Double> smaValues = sma.calculate(series);
        
        for (int i = 0; i < series.getBarCount(); i++) {
            assertEquals(smaValues.get(i), middleBand.get(i));
        }
        
        // Get all bands
        List<List<Double>> bands = bb.calculateBands(series);
        List<Double> upperBand = bands.get(0);
        List<Double> middleBandValues = bands.get(1);
        List<Double> lowerBand = bands.get(2);
        
        // Verify bands lengths
        assertEquals(series.getBarCount(), upperBand.size());
        assertEquals(series.getBarCount(), middleBandValues.size());
        assertEquals(series.getBarCount(), lowerBand.size());
        
        // Verify bands relationships
        for (int i = 0; i < series.getBarCount(); i++) {
            if (!Double.isNaN(middleBandValues.get(i))) {
                assertTrue(upperBand.get(i) > middleBandValues.get(i));
                assertTrue(lowerBand.get(i) < middleBandValues.get(i));
                
                // Check bands are equidistant from middle band
                double upperDiff = upperBand.get(i) - middleBandValues.get(i);
                double lowerDiff = middleBandValues.get(i) - lowerBand.get(i);
                assertEquals(upperDiff, lowerDiff, 0.001);
            }
        }
        
        // Test percent B calculation
        List<Double> percentB = bb.calculatePercentB(series);
        assertEquals(series.getBarCount(), percentB.size());
        
        // Test bandwidth calculation
        List<Double> bandwidth = bb.calculateBandwidth(series);
        assertEquals(series.getBarCount(), bandwidth.size());
    }
}
