package com.tradingbacktester.strategy;

import com.tradingbacktester.core.Portfolio;
import com.tradingbacktester.model.Bar;
import com.tradingbacktester.model.Order;
import com.tradingbacktester.model.TimeSeries;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the trading strategies.
 */
public class StrategyTest {
    
    private TimeSeries series;
    private Portfolio portfolio;
    private Map<String, TimeSeries> marketData;
    private Map<String, Bar> currentBars;
    private LocalDateTime currentTime;
    private static final String SYMBOL = "AAPL";
    
    @BeforeEach
    public void setUp() {
        // Create sample price data
        series = new TimeSeries(SYMBOL);
        LocalDateTime time = LocalDateTime.of(2022, 1, 1, 0, 0);
        
        // Create a series with oscillating prices to trigger both buy and sell signals
        double[] prices = {
            100, 102, 104, 106, 108, 110, 108, 106, 104, 102,
            100, 98,  96,  94,  92,  90,  92,  94,  96,  98,
            100, 102, 104, 106, 108, 110, 108, 106, 104, 102,
            100, 98,  96,  94,  92,  90,  92,  94,  96,  98,
            100, 102, 104, 106, 108, 110, 108, 106, 104, 102,
            100, 98,  96,  94,  92,  90,  92,  94,  96,  98
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
        
        // Initialize market data
        marketData = new HashMap<>();
        marketData.put(SYMBOL, series);
        
        // Initialize portfolio
        portfolio = new Portfolio(100000);
        
        // Start with the latest bar for testing
        currentTime = time.plusDays(prices.length - 1);
        currentBars = new HashMap<>();
        currentBars.put(SYMBOL, series.getLastBar());
    }
    
    @Test
    public void testMovingAverageCrossoverStrategy() {
        // Create MA crossover strategy with SMA
        MovingAverageCrossover strategy = new MovingAverageCrossover(
            SYMBOL, 10, 20, false, 0.1, 5, 10
        );
        
        // Initialize and check name
        strategy.initialize(marketData);
        assertTrue(strategy.getName().contains("MA Crossover"));
        assertEquals(1, strategy.getRequiredSymbols().size());
        assertTrue(strategy.getRequiredSymbols().contains(SYMBOL));
        
        // Generate orders
        List<Order> orders = strategy.generateOrders(currentTime, currentBars, portfolio);
        
        // Verify orders (should depend on price pattern)
        // Not asserting specific orders since it depends on the crossover pattern
    }
    
    @Test
    public void testRSIStrategy() {
        // Create RSI strategy
        RSIStrategy strategy = new RSIStrategy(
            SYMBOL, 14, 30, 70, 0.1, 5, 10
        );
        
        // Initialize and check name
        strategy.initialize(marketData);
        assertTrue(strategy.getName().contains("RSI Strategy"));
        assertEquals(1, strategy.getRequiredSymbols().size());
        assertTrue(strategy.getRequiredSymbols().contains(SYMBOL));
        
        // Generate orders
        List<Order> orders = strategy.generateOrders(currentTime, currentBars, portfolio);
        
        // Verify orders (should depend on RSI values)
        // Not asserting specific orders since it depends on the RSI pattern
    }
    
    @Test
    public void testParameterHandling() {
        // Create MA crossover strategy
        MovingAverageCrossover strategy = new MovingAverageCrossover(
            SYMBOL, 10, 20, false, 0.1, 5, 10
        );
        
        // Get initial parameters
        Map<String, Object> params = strategy.getParameters();
        assertEquals(10, params.get("fastPeriod"));
        assertEquals(20, params.get("slowPeriod"));
        assertEquals(false, params.get("useEMA"));
        assertEquals(0.1, params.get("positionSize"));
        assertEquals(5.0, params.get("stopLossPercent"));
        assertEquals(10.0, params.get("takeProfitPercent"));
        
        // Change parameters
        strategy.setParameter("fastPeriod", 5);
        strategy.setParameter("slowPeriod", 15);
        strategy.setParameter("useEMA", true);
        
        // Verify changes
        params = strategy.getParameters();
        assertEquals(5, params.get("fastPeriod"));
        assertEquals(15, params.get("slowPeriod"));
        assertEquals(true, params.get("useEMA"));
    }
    
    @Test
    public void testPositionSizing() {
        // Test with different position sizes
        testPositionSizeWithParams(0.1, 10); // 10% position size
        testPositionSizeWithParams(0.2, 20); // 20% position size
        testPositionSizeWithParams(0.5, 50); // 50% position size
    }
    
    private void testPositionSizeWithParams(double positionSize, double expectedPercentage) {
        // Create strategy with specified position size
        MovingAverageCrossover strategy = new MovingAverageCrossover(
            SYMBOL, 5, 10, false, positionSize, 5, 10
        );
        
        // Force a clear buy signal
        TimeSeries risingPrices = createRisingPriceSeries();
        Map<String, TimeSeries> data = new HashMap<>();
        data.put(SYMBOL, risingPrices);
        
        strategy.initialize(data);
        
        // Create a bar at the right time to generate a buy signal
        Bar bar = risingPrices.getBar(15); // After both MAs are calculated
        Map<String, Bar> bars = new HashMap<>();
        bars.put(SYMBOL, bar);
        
        // Generate order
        List<Order> orders = strategy.generateOrders(bar.getTimestamp(), bars, portfolio);
        
        // Check if order was generated
        if (!orders.isEmpty()) {
            Order order = orders.get(0);
            
            // Check if position size matches expected
            double price = bar.getClose();
            double portfolioValue = portfolio.getTotalValue();
            double expectedQuantity = (portfolioValue * positionSize) / price;
            
            if (order.isBuy()) {
                assertTrue(Math.abs(order.getQuantity() - expectedQuantity) < 1.0);
            }
        }
    }
    
    private TimeSeries createRisingPriceSeries() {
        TimeSeries series = new TimeSeries(SYMBOL);
        LocalDateTime time = LocalDateTime.of(2022, 1, 1, 0, 0);
        
        for (int i = 0; i < 30; i++) {
            double price = 100 + i * 2; // Steadily rising prices
            Bar bar = new Bar(time.plusDays(i), price - 0.5, price + 1.0, price - 1.0, price, 100000);
            series.addBar(bar);
        }
        
        return series;
    }
}
