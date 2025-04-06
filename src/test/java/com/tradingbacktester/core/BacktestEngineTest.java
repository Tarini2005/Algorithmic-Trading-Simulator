package com.tradingbacktester.core;

import com.tradingbacktester.model.Bar;
import com.tradingbacktester.model.Order;
import com.tradingbacktester.model.OrderType;
import com.tradingbacktester.model.TimeSeries;
import com.tradingbacktester.strategy.Strategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the backtesting engine.
 */
public class BacktestEngineTest {
    
    private BacktestEngine engine;
    private MarketDataService marketDataService;
    private MockStrategy strategy;
    private static final double INITIAL_CAPITAL = 10000.0;
    
    @BeforeEach
    public void setUp() {
        MarketDataLoader dataLoader = new MockMarketDataLoader();
        marketDataService = new MarketDataService(dataLoader);
        engine = new BacktestEngine(marketDataService, INITIAL_CAPITAL);
        strategy = new MockStrategy("AAPL");
        engine.addStrategy(strategy);
    }
    
    @Test
    public void testBacktestWithNoTrades() {
        // Run backtest with no trades
        strategy.setEmitOrders(false);
        
        LocalDateTime startTime = LocalDateTime.of(2022, 1, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2022, 1, 31, 0, 0);
        
        Map<String, Object> results = engine.runBacktest(startTime, endTime);
        
        // Verify results
        assertEquals(INITIAL_CAPITAL, results.get("initialCapital"));
        assertEquals(INITIAL_CAPITAL, results.get("finalCapital"));
        assertEquals(0.0, (Double) results.get("profit"));
        assertEquals(0.0, (Double) results.get("returnPct"));
        
        @SuppressWarnings("unchecked")
        List<Trade> trades = (List<Trade>) results.get("trades");
        assertTrue(trades.isEmpty());
    }
    
    @Test
    public void testBacktestWithLongTrade() {
        // Set up mock strategy to emit buy order
        strategy.setEmitOrders(true);
        strategy.setOrderType(OrderType.MARKET);
        strategy.setOrderQuantity(10); // Buy 10 shares
        
        LocalDateTime startTime = LocalDateTime.of(2022, 1, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2022, 1, 31, 0, 0);
        
        Map<String, Object> results = engine.runBacktest(startTime, endTime);
        
        // Verify results
        assertEquals(INITIAL_CAPITAL, results.get("initialCapital"));
        assertTrue((Double) results.get("finalCapital") > INITIAL_CAPITAL); // Should have profit
        assertTrue((Double) results.get("profit") > 0);
        assertTrue((Double) results.get("returnPct") > 0);
        
        @SuppressWarnings("unchecked")
        List<Trade> trades = (List<Trade>) results.get("trades");
        assertEquals(1, trades.size());
        
        Trade trade = trades.get(0);
        assertEquals("AAPL", trade.getSymbol());
        assertTrue(trade.isLong());
        assertTrue(trade.getProfit() > 0);
    }
    
    @Test
    public void testBacktestWithShortTrade() {
        // Set up mock strategy to emit sell order
        strategy.setEmitOrders(true);
        strategy.setOrderType(OrderType.MARKET);
        strategy.setOrderQuantity(-10); // Sell 10 shares short
        
        LocalDateTime startTime = LocalDateTime.of(2022, 1, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2022, 1, 31, 0, 0);
        
        Map<String, Object> results = engine.runBacktest(startTime, endTime);
        
        // Short trades are disabled by default, so no trades should be executed
        @SuppressWarnings("unchecked")
        List<Trade> trades = (List<Trade>) results.get("trades");
        assertTrue(trades.isEmpty());
    }
    
    @Test
    public void testCommissionsImpactPerformance() {
        // Run with no commissions
        engine.setCommissionRate(0.0);
        strategy.setEmitOrders(true);
        strategy.setOrderType(OrderType.MARKET);
        strategy.setOrderQuantity(10);
        
        LocalDateTime startTime = LocalDateTime.of(2022, 1, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2022, 1, 31, 0, 0);
        
        Map<String, Object> resultsNoCommission = engine.runBacktest(startTime, endTime);
        double profitNoCommission = (Double) resultsNoCommission.get("profit");
        
        // Run with commissions
        engine.setCommissionRate(0.01); // 1% commission
        Map<String, Object> resultsWithCommission = engine.runBacktest(startTime, endTime);
        double profitWithCommission = (Double) resultsWithCommission.get("profit");
        
        // Verify commission reduces profit
        assertTrue(profitWithCommission < profitNoCommission);
    }
    
    @Test
    public void testSlippageImpactPerformance() {
        // Run with no slippage
        engine.setSlippageModel(0.0);
        strategy.setEmitOrders(true);
        strategy.setOrderType(OrderType.MARKET);
        strategy.setOrderQuantity(10);
        
        LocalDateTime startTime = LocalDateTime.of(2022, 1, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2022, 1, 31, 0, 0);
        
        Map<String, Object> resultsNoSlippage = engine.runBacktest(startTime, endTime);
        double profitNoSlippage = (Double) resultsNoSlippage.get("profit");
        
        // Run with slippage
        engine.setSlippageModel(0.01); // 1% slippage
        Map<String, Object> resultsWithSlippage = engine.runBacktest(startTime, endTime);
        double profitWithSlippage = (Double) resultsWithSlippage.get("profit");
        
        // Verify slippage reduces profit
        assertTrue(profitWithSlippage < profitNoSlippage);
    }
    
    /**
     * Mock market data loader for testing.
     */
    private static class MockMarketDataLoader extends MarketDataLoader {
        @Override
        public TimeSeries loadData(String symbol, LocalDateTime startTime, LocalDateTime endTime) {
            TimeSeries series = new TimeSeries(symbol);
            
            // Create simple rising price data
            LocalDateTime time = startTime;
            double price = 150.0;
            
            while (!time.isAfter(endTime)) {
                // Add some randomness
                double noise = Math.random() * 2 - 1; // -1 to 1
                double open = price + noise;
                double high = open + Math.random() * 2;
                double low = open - Math.random() * 2;
                double close = open + Math.random();
                
                Bar bar = new Bar(time, open, high, low, close, 100000);
                series.addBar(bar);
                
                price += 0.5; // Trending upward
                time = time.plusDays(1);
            }
            
            return series;
        }
    }
    
    /**
     * Mock strategy for testing.
     */
    private static class MockStrategy implements Strategy {
        private final String symbol;
        private boolean emitOrders;
        private OrderType orderType;
        private double orderQuantity;
        
        public MockStrategy(String symbol) {
            this.symbol = symbol;
            this.emitOrders = false;
            this.orderType = OrderType.MARKET;
            this.orderQuantity = 0;
        }
        
        public void setEmitOrders(boolean emitOrders) {
            this.emitOrders = emitOrders;
        }
        
        public void setOrderType(OrderType orderType) {
            this.orderType = orderType;
        }
        
        public void setOrderQuantity(double orderQuantity) {
            this.orderQuantity = orderQuantity;
        }
        
        @Override
        public String getName() {
            return "MockStrategy";
        }
        
        @Override
        public Set<String> getRequiredSymbols() {
            return Collections.singleton(symbol);
        }
        
        @Override
        public void initialize(Map<String, TimeSeries> data) {
            // No initialization needed for tests
        }
        
        @Override
        public void onBar(LocalDateTime timestamp, Map<String, Bar> currentBars, Portfolio portfolio) {
            // No action needed on bar
        }
        
        @Override
        public List<Order> generateOrders(LocalDateTime timestamp, Map<String, Bar> currentBars, Portfolio portfolio) {
            if (!emitOrders) {
                return Collections.emptyList();
            }
            
            // Create order based on settings
            Order order = new Order(symbol, orderType, orderQuantity, timestamp);
            return Collections.singletonList(order);
        }
        
        @Override
        public Map<String, Object> getParameters() {
            return Collections.emptyMap();
        }
        
        @Override
        public void setParameter(String name, Object value) {
            // No parameters to set for tests
        }
    }
}
