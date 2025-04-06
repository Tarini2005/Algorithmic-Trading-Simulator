package com.tradingbacktester.strategy;

import com.tradingbacktester.core.BacktestEngine;
import com.tradingbacktester.core.MarketDataService;
import com.tradingbacktester.core.Trade;
import com.tradingbacktester.model.TimeSeries;
import com.tradingbacktester.risk.RiskAnalyzer;
import com.tradingbacktester.risk.RiskMetrics;
import com.tradingbacktester.utils.MathUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * Evaluates and optimizes trading strategies by running multiple backtests with different parameters.
 */
public class StrategyEvaluator {
    
    private final MarketDataService marketDataService;
    private final ExecutorService executorService;
    
    /**
     * Creates a new strategy evaluator.
     * 
     * @param marketDataService the market data service
     */
    public StrategyEvaluator(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
        this.executorService = Executors.newFixedThreadPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() - 1)
        );
    }
    
    /**
     * Evaluates a strategy with multiple parameter combinations.
     * 
     * @param strategyFactory the strategy factory
     * @param parameterSets the list of parameter sets to test
     * @param symbol the symbol to test
     * @param startTime the start time of the backtest
     * @param endTime the end time of the backtest
     * @param initialCapital the initial capital
     * @param commissionRate the commission rate
     * @param slippageModel the slippage model
     * @return the evaluation results
     * @throws InterruptedException if interrupted while waiting for results
     * @throws ExecutionException if an exception occurs during execution
     */
    public List<EvaluationResult> evaluateParameters(
            StrategyFactory strategyFactory,
            List<Map<String, Object>> parameterSets,
            String symbol,
            LocalDateTime startTime,
            LocalDateTime endTime,
            double initialCapital,
            double commissionRate,
            double slippageModel) throws InterruptedException, ExecutionException {
        
        // Preload market data
        TimeSeries marketData = marketDataService.getHistoricalData(symbol, startTime, endTime);
        
        // Submit tasks for each parameter set
        List<Future<EvaluationResult>> futures = new ArrayList<>();
        
        for (Map<String, Object> params : parameterSets) {
            futures.add(executorService.submit(() -> {
                // Create strategy with the current parameters
                Strategy strategy = strategyFactory.createStrategy(symbol, params);
                
                // Create backtest engine
                BacktestEngine engine = new BacktestEngine(marketDataService, initialCapital);
                engine.setCommissionRate(commissionRate);
                engine.setSlippageModel(slippageModel);
                engine.addStrategy(strategy);
                
                // Run backtest
                Map<String, Object> results = engine.runBacktest(startTime, endTime);
                
                // Create evaluation result
                EvaluationResult result = new EvaluationResult();
                result.setParameters(new HashMap<>(params));
                result.setMetrics(calculateMetrics(results));
                result.setTrades((List<Trade>) results.get("trades"));
                
                return result;
            }));
        }
        
        // Collect results
        List<EvaluationResult> results = new ArrayList<>();
        for (Future<EvaluationResult> future : futures) {
            results.add(future.get());
        }
        
        // Sort results by return
        results.sort((r1, r2) -> Double.compare(r2.getMetrics().getTotalReturn(), r1.getMetrics().getTotalReturn()));
        
        return results;
    }
    
    /**
     * Optimizes strategy parameters using walk-forward analysis.
     * 
     * @param strategyFactory the strategy factory
     * @param parameterSets the list of parameter sets to test
     * @param symbol the symbol to test
     * @param startTime the start time of the analysis
     * @param endTime the end time of the analysis
     * @param trainPeriodDays the number of days in each training period
     * @param testPeriodDays the number of days in each test period
     * @param initialCapital the initial capital
     * @param commissionRate the commission rate
     * @param slippageModel the slippage model
     * @return the walk-forward optimization results
     * @throws InterruptedException if interrupted while waiting for results
     * @throws ExecutionException if an exception occurs during execution
     */
    public WalkForwardResult walkForwardOptimization(
            StrategyFactory strategyFactory,
            List<Map<String, Object>> parameterSets,
            String symbol,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int trainPeriodDays,
            int testPeriodDays,
            double initialCapital,
            double commissionRate,
            double slippageModel) throws InterruptedException, ExecutionException {
        
        // Preload market data
        TimeSeries marketData = marketDataService.getHistoricalData(symbol, startTime, endTime);
        
        // Create periods for walk-forward analysis
        List<Period> periods = createWalkForwardPeriods(startTime, endTime, trainPeriodDays, testPeriodDays);
        
        // Results for each period
        List<PeriodResult> periodResults = new ArrayList<>();
        List<Trade> allTrades = new ArrayList<>();
        Map<String, Integer> parameterUsageCount = new HashMap<>();
        
        // Process each period
        for (Period period : periods) {
            // Evaluate strategies on training period
            List<EvaluationResult> trainingResults = evaluateParameters(
                strategyFactory, parameterSets, symbol,
                period.trainStart, period.trainEnd,
                initialCapital, commissionRate, slippageModel
            );
            
            // Get best parameters from training
            Map<String, Object> bestParams = trainingResults.get(0).getParameters();
            
            // Count parameter usage
            for (Map.Entry<String, Object> entry : bestParams.entrySet()) {
                String key = entry.getKey() + ":" + entry.getValue();
                parameterUsageCount.put(key, parameterUsageCount.getOrDefault(key, 0) + 1);
            }
            
            // Test best parameters on test period
            Strategy strategy = strategyFactory.createStrategy(symbol, bestParams);
            BacktestEngine engine = new BacktestEngine(marketDataService, initialCapital);
            engine.setCommissionRate(commissionRate);
            engine.setSlippageModel(slippageModel);
            engine.addStrategy(strategy);
            
            Map<String, Object> testResults = engine.runBacktest(period.testStart, period.testEnd);
            
            // Create period result
            PeriodResult periodResult = new PeriodResult();
            periodResult.setPeriod(period);
            periodResult.setBestParameters(bestParams);
            periodResult.setTestMetrics(calculateMetrics(testResults));
            periodResult.setTestTrades((List<Trade>) testResults.get("trades"));
            
            periodResults.add(periodResult);
            allTrades.addAll(periodResult.getTestTrades());
        }
        
        // Calculate overall performance
        RiskAnalyzer riskAnalyzer = new RiskAnalyzer();
        RiskMetrics overallMetrics = riskAnalyzer.calculateRiskMetrics(allTrades, initialCapital);
        
        // Create walk-forward result
        WalkForwardResult result = new WalkForwardResult();
        result.setPeriodResults(periodResults);
        result.setOverallMetrics(overallMetrics);
        result.setParameterUsageCount(parameterUsageCount);
        
        return result;
    }
    
    /**
     * Creates periods for walk-forward analysis.
     * 
     * @param startTime the start time
     * @param endTime the end time
     * @param trainPeriodDays the number of days in each training period
     * @param testPeriodDays the number of days in each test period
     * @return the list of periods
     */
    private List<Period> createWalkForwardPeriods(
            LocalDateTime startTime, LocalDateTime endTime,
            int trainPeriodDays, int testPeriodDays) {
        
        List<Period> periods = new ArrayList<>();
        LocalDateTime currentStart = startTime;
        
        while (currentStart.plusDays(trainPeriodDays + testPeriodDays).isBefore(endTime)) {
            LocalDateTime trainEnd = currentStart.plusDays(trainPeriodDays);
            LocalDateTime testStart = trainEnd;
            LocalDateTime testEnd = testStart.plusDays(testPeriodDays);
            
            Period period = new Period(currentStart, trainEnd, testStart, testEnd);
            periods.add(period);
            
            currentStart = testEnd;
        }
        
        return periods;
    }
    
    /**
     * Calculates risk metrics from backtest results.
     * 
     * @param results the backtest results
     * @return the risk metrics
     */
    private RiskMetrics calculateMetrics(Map<String, Object> results) {
        RiskMetrics metrics = new RiskMetrics();
        
        // Extract basic metrics
        double initialCapital = (Double) results.get("initialCapital");
        double finalCapital = (Double) results.get("finalCapital");
        double profit = (Double) results.get("profit");
        double returnPct = (Double) results.get("returnPct");
        double maxDrawdown = (Double) results.get("maxDrawdown");
        int totalTrades = (Integer) results.get("totalTrades");
        int winningTrades = (Integer) results.get("winningTrades");
        double profitFactor = (Double) results.getOrDefault("profitFactor", 0.0);
        
        // Calculate additional metrics
        double totalReturn = profit / initialCapital;
        double winRate = totalTrades > 0 ? (double) winningTrades / totalTrades : 0;
        
        // Set metrics
        metrics.setTotalReturn(totalReturn);
        metrics.setMaxDrawdown(maxDrawdown / 100.0); // Convert from percentage
        metrics.setWinRate(winRate);
        metrics.setProfitFactor(profitFactor);
        metrics.setNumberOfTrades(totalTrades);
        
        // Calculate Sharpe ratio and other metrics if we have trades
        @SuppressWarnings("unchecked")
        List<Trade> trades = (List<Trade>) results.get("trades");
        if (trades != null && !trades.isEmpty()) {
            RiskAnalyzer riskAnalyzer = new RiskAnalyzer();
            RiskMetrics fullMetrics = riskAnalyzer.calculateRiskMetrics(trades, initialCapital);
            
            metrics.setSharpeRatio(fullMetrics.getSharpeRatio());
            metrics.setVolatility(fullMetrics.getVolatility());
            metrics.setSortinoRatio(fullMetrics.getSortinoRatio());
            metrics.setCalmarRatio(fullMetrics.getCalmarRatio());
            metrics.setExpectancy(fullMetrics.getExpectancy());
        }
        
        return metrics;
    }
    
    /**
     * Shuts down the executor service.
     */
    public void shutdown() {
        executorService.shutdown();
    }
    
    /**
     * Represents a strategy factory for creating strategies with different parameters.
     */
    public interface StrategyFactory {
        /**
         * Creates a strategy with the specified parameters.
         * 
         * @param symbol the symbol
         * @param parameters the parameters
         * @return the strategy
         */
        Strategy createStrategy(String symbol, Map<String, Object> parameters);
    }
    
    /**
     * Represents a period for walk-forward analysis.
     */
    public static class Period {
        private final LocalDateTime trainStart;
        private final LocalDateTime trainEnd;
        private final LocalDateTime testStart;
        private final LocalDateTime testEnd;
        
        /**
         * Creates a new period.
         * 
         * @param trainStart the training start time
         * @param trainEnd the training end time
         * @param testStart the test start time
         * @param testEnd the test end time
         */
        public Period(LocalDateTime trainStart, LocalDateTime trainEnd, LocalDateTime testStart, LocalDateTime testEnd) {
            this.trainStart = trainStart;
            this.trainEnd = trainEnd;
            this.testStart = testStart;
            this.testEnd = testEnd;
        }
        
        /**
         * Gets the training start time.
         * 
         * @return the training start time
         */
        public LocalDateTime getTrainStart() {
            return trainStart;
        }
        
        /**
         * Gets the training end time.
         * 
         * @return the training end time
         */
        public LocalDateTime getTrainEnd() {
            return trainEnd;
        }
        
        /**
         * Gets the test start time.
         * 
         * @return the test start time
         */
        public LocalDateTime getTestStart() {
            return testStart;
        }
        
        /**
         * Gets the test end time.
         * 
         * @return the test end time
         */
        public LocalDateTime getTestEnd() {
            return testEnd;
        }
        
        @Override
        public String toString() {
            return "Train: " + trainStart.toLocalDate() + " to " + trainEnd.toLocalDate() +
                   ", Test: " + testStart.toLocalDate() + " to " + testEnd.toLocalDate();
        }
    }
    
    /**
     * Represents an evaluation result for a single parameter set.
     */
    public static class EvaluationResult {
        private Map<String, Object> parameters;
        private RiskMetrics metrics;
        private List<Trade> trades;
        
        /**
         * Gets the parameters.
         * 
         * @return the parameters
         */
        public Map<String, Object> getParameters() {
            return parameters;
        }
        
        /**
         * Sets the parameters.
         * 
         * @param parameters the parameters
         */
        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }
        
        /**
         * Gets the risk metrics.
         * 
         * @return the risk metrics
         */
        public RiskMetrics getMetrics() {
            return metrics;
        }
        
        /**
         * Sets the risk metrics.
         * 
         * @param metrics the risk metrics
         */
        public void setMetrics(RiskMetrics metrics) {
            this.metrics = metrics;
        }
        
        /**
         * Gets the trades.
         * 
         * @return the trades
         */
        public List<Trade> getTrades() {
            return trades;
        }
        
        /**
         * Sets the trades.
         * 
         * @param trades the trades
         */
        public void setTrades(List<Trade> trades) {
            this.trades = trades;
        }
        
        @Override
        public String toString() {
            return "Parameters: " + parameters +
                   ", Return: " + String.format("%.2f%%", metrics.getTotalReturn() * 100) +
                   ", Trades: " + metrics.getNumberOfTrades();
        }
    }
    
    /**
     * Represents a period result for walk-forward analysis.
     */
    public static class PeriodResult {
        private Period period;
        private Map<String, Object> bestParameters;
        private RiskMetrics testMetrics;
        private List<Trade> testTrades;
        
        /**
         * Gets the period.
         * 
         * @return the period
         */
        public Period getPeriod() {
            return period;
        }
        
        /**
         * Sets the period.
         * 
         * @param period the period
         */
        public void setPeriod(Period period) {
            this.period = period;
        }
        
        /**
         * Gets the best parameters.
         * 
         * @return the best parameters
         */
        public Map<String, Object> getBestParameters() {
            return bestParameters;
        }
        
        /**
         * Sets the best parameters.
         * 
         * @param bestParameters the best parameters
         */
        public void setBestParameters(Map<String, Object> bestParameters) {
            this.bestParameters = bestParameters;
        }
        
        /**
         * Gets the test metrics.
         * 
         * @return the test metrics
         */
        public RiskMetrics getTestMetrics() {
            return testMetrics;
        }
        
        /**
         * Sets the test metrics.
         * 
         * @param testMetrics the test metrics
         */
        public void setTestMetrics(RiskMetrics testMetrics) {
            this.testMetrics = testMetrics;
        }
        
        /**
         * Gets the test trades.
         * 
         * @return the test trades
         */
        public List<Trade> getTestTrades() {
            return testTrades;
        }
        
        /**
         * Sets the test trades.
         * 
         * @param testTrades the test trades
         */
        public void setTestTrades(List<Trade> testTrades) {
            this.testTrades = testTrades;
        }
        
        @Override
        public String toString() {
            return "Period: " + period +
                   ", Best Parameters: " + bestParameters +
                   ", Test Return: " + String.format("%.2f%%", testMetrics.getTotalReturn() * 100) +
                   ", Test Trades: " + testMetrics.getNumberOfTrades();
        }
    }
    
    /**
     * Represents a walk-forward optimization result.
     */
    public static class WalkForwardResult {
        private List<PeriodResult> periodResults;
        private RiskMetrics overallMetrics;
        private Map<String, Integer> parameterUsageCount;
        
        /**
         * Gets the period results.
         * 
         * @return the period results
         */
        public List<PeriodResult> getPeriodResults() {
            return periodResults;
        }
        
        /**
         * Sets the period results.
         * 
         * @param periodResults the period results
         */
        public void setPeriodResults(List<PeriodResult> periodResults) {
            this.periodResults = periodResults;
        }
        
        /**
         * Gets the overall metrics.
         * 
         * @return the overall metrics
         */
        public RiskMetrics getOverallMetrics() {
            return overallMetrics;
        }
        
        /**
         * Sets the overall metrics.
         * 
         * @param overallMetrics the overall metrics
         */
        public void setOverallMetrics(RiskMetrics overallMetrics) {
            this.overallMetrics = overallMetrics;
        }
        
        /**
         * Gets the parameter usage count.
         * 
         * @return the parameter usage count
         */
        public Map<String, Integer> getParameterUsageCount() {
            return parameterUsageCount;
        }
        
        /**
         * Sets the parameter usage count.
         * 
         * @param parameterUsageCount the parameter usage count
         */
        public void setParameterUsageCount(Map<String, Integer> parameterUsageCount) {
            this.parameterUsageCount = parameterUsageCount;
        }
        
        /**
         * Gets the most frequently used parameters.
         * 
         * @return a map of parameter name to most frequent value
         */
        public Map<String, Object> getMostFrequentParameters() {
            Map<String, Map<Object, Integer>> parameterValueCounts = new HashMap<>();
            
            // Count parameter values
            for (PeriodResult result : periodResults) {
                for (Map.Entry<String, Object> entry : result.getBestParameters().entrySet()) {
                    String paramName = entry.getKey();
                    Object paramValue = entry.getValue();
                    
                    parameterValueCounts.putIfAbsent(paramName, new HashMap<>());
                    Map<Object, Integer> valueCounts = parameterValueCounts.get(paramName);
                    valueCounts.put(paramValue, valueCounts.getOrDefault(paramValue, 0) + 1);
                }
            }
            
            // Find most frequent value for each parameter
            Map<String, Object> mostFrequentParams = new HashMap<>();
            for (Map.Entry<String, Map<Object, Integer>> entry : parameterValueCounts.entrySet()) {
                String paramName = entry.getKey();
                Map<Object, Integer> valueCounts = entry.getValue();
                
                Object mostFrequentValue = Collections.max(
                    valueCounts.entrySet(),
                    Map.Entry.comparingByValue()
                ).getKey();
                
                mostFrequentParams.put(paramName, mostFrequentValue);
            }
            
            return mostFrequentParams;
        }
        
        @Override
        public String toString() {
            return "Walk-Forward Result: " +
                   periodResults.size() + " periods, " +
                   "Overall Return: " + String.format("%.2f%%", overallMetrics.getTotalReturn() * 100) +
                   ", Overall Sharpe: " + String.format("%.2f", overallMetrics.getSharpeRatio());
        }
    }
}
