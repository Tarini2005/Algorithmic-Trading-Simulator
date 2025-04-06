package com.tradingbacktester.risk;

import com.tradingbacktester.core.Portfolio;
import com.tradingbacktester.core.Trade;
import com.tradingbacktester.utils.MathUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Analyzes and calculates risk metrics for trading strategies.
 */
public class RiskAnalyzer {
    
    /**
     * Calculates risk metrics for a list of trades.
     * 
     * @param trades the trades
     * @param initialCapital the initial capital
     * @return the risk metrics
     */
    public RiskMetrics calculateRiskMetrics(List<Trade> trades, double initialCapital) {
        if (trades == null || trades.isEmpty()) {
            return new RiskMetrics();
        }
        
        // Calculate equity curve
        List<Double> equityCurve = new ArrayList<>();
        equityCurve.add(initialCapital);
        
        double equity = initialCapital;
        for (Trade trade : trades) {
            equity += trade.getProfit();
            equityCurve.add(equity);
        }
        
        // Calculate returns
        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < equityCurve.size(); i++) {
            double returnPct = (equityCurve.get(i) - equityCurve.get(i - 1)) / equityCurve.get(i - 1);
            returns.add(returnPct);
        }
        
        // Calculate metrics
        double totalReturn = (equityCurve.get(equityCurve.size() - 1) - initialCapital) / initialCapital;
        double maxDrawdown = MathUtils.maxDrawdown(equityCurve);
        double sharpeRatio = MathUtils.sharpeRatio(returns, 0.02, 252); // Assuming daily returns and 2% risk-free rate
        double volatility = MathUtils.standardDeviation(returns) * Math.sqrt(252); // Annualized volatility
        double sortinoRatio = calculateSortinoRatio(returns, 0.02, 252);
        double calmarRatio = totalReturn / maxDrawdown;
        
        // Calculate win rate
        int winCount = 0;
        int lossCount = 0;
        double totalProfit = 0;
        double totalLoss = 0;
        
        for (Trade trade : trades) {
            if (trade.getProfit() > 0) {
                winCount++;
                totalProfit += trade.getProfit();
            } else {
                lossCount++;
                totalLoss += Math.abs(trade.getProfit());
            }
        }
        
        double winRate = (double) winCount / trades.size();
        double profitFactor = totalLoss > 0 ? totalProfit / totalLoss : Double.POSITIVE_INFINITY;
        double avgWin = winCount > 0 ? totalProfit / winCount : 0;
        double avgLoss = lossCount > 0 ? totalLoss / lossCount : 0;
        double expectancy = (winRate * avgWin) - ((1 - winRate) * avgLoss);
        
        // Create metrics object
        RiskMetrics metrics = new RiskMetrics();
        metrics.setTotalReturn(totalReturn);
        metrics.setMaxDrawdown(maxDrawdown);
        metrics.setSharpeRatio(sharpeRatio);
        metrics.setVolatility(volatility);
        metrics.setSortinoRatio(sortinoRatio);
        metrics.setCalmarRatio(calmarRatio);
        metrics.setWinRate(winRate);
        metrics.setProfitFactor(profitFactor);
        metrics.setExpectancy(expectancy);
        metrics.setNumberOfTrades(trades.size());
        
        return metrics;
    }
    
    /**
     * Calculates the Sortino ratio.
     * 
     * @param returns the period returns
     * @param riskFreeRate the risk-free rate
     * @param periodsPerYear the number of periods in a year
     * @return the Sortino ratio
     */
    private double calculateSortinoRatio(List<Double> returns, double riskFreeRate, int periodsPerYear) {
        if (returns == null || returns.isEmpty()) {
            return Double.NaN;
        }
        
        // Convert annual risk-free rate to period rate
        double periodRiskFreeRate = Math.pow(1 + riskFreeRate, 1.0 / periodsPerYear) - 1;
        
        // Calculate downside returns (negative returns)
        List<Double> downsideReturns = new ArrayList<>();
        for (Double ret : returns) {
            if (ret < periodRiskFreeRate) {
                downsideReturns.add(ret - periodRiskFreeRate);
            }
        }
        
        // Calculate average return
        double avgReturn = 0;
        for (Double ret : returns) {
            avgReturn += ret;
        }
        avgReturn /= returns.size();
        
        // Calculate downside deviation
        double sumSquaredDownside = 0;
        for (Double downside : downsideReturns) {
            sumSquaredDownside += downside * downside;
        }
        
        double downsideDeviation = Math.sqrt(sumSquaredDownside / returns.size());
        
        // Calculate and return annualized Sortino ratio
        if (downsideDeviation == 0 || Double.isNaN(downsideDeviation)) {
            return Double.POSITIVE_INFINITY;
        }
        
        return ((avgReturn - periodRiskFreeRate) / downsideDeviation) * Math.sqrt(periodsPerYear);
    }
    
    /**
     * Calculates the position size based on risk per trade.
     * 
     * @param portfolio the portfolio
     * @param price the current price
     * @param stopLossPrice the stop loss price
     * @param riskPercent the risk percent per trade
     * @return the position size
     */
    public int calculatePositionSize(Portfolio portfolio, double price, double stopLossPrice, double riskPercent) {
        if (price <= 0 || stopLossPrice <= 0 || riskPercent <= 0) {
            return 0;
        }
        
        // Calculate risk amount
        double riskAmount = portfolio.getTotalValue() * (riskPercent / 100.0);
        
        // Calculate risk per share
        double riskPerShare = Math.abs(price - stopLossPrice);
        
        // Calculate position size
        int positionSize = (int) (riskAmount / riskPerShare);
        
        return positionSize;
    }
    
    /**
     * Calculates the stop loss price based on ATR (Average True Range).
     * 
     * @param price the current price
     * @param atr the ATR value
     * @param multiplier the ATR multiplier
     * @param isLong true for long position, false for short position
     * @return the stop loss price
     */
    public double calculateAtrStopLoss(double price, double atr, double multiplier, boolean isLong) {
        if (isLong) {
            return price - (atr * multiplier);
        } else {
            return price + (atr * multiplier);
        }
    }
}
