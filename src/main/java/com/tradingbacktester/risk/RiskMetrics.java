package com.tradingbacktester.risk;

/**
 * Contains various risk and performance metrics for a trading system.
 */
public class RiskMetrics {
    private double totalReturn;
    private double maxDrawdown;
    private double sharpeRatio;
    private double volatility;
    private double sortinoRatio;
    private double calmarRatio;
    private double winRate;
    private double profitFactor;
    private double expectancy;
    private int numberOfTrades;
    
    /**
     * Creates a new risk metrics object with default values.
     */
    public RiskMetrics() {
        this.totalReturn = 0;
        this.maxDrawdown = 0;
        this.sharpeRatio = 0;
        this.volatility = 0;
        this.sortinoRatio = 0;
        this.calmarRatio = 0;
        this.winRate = 0;
        this.profitFactor = 0;
        this.expectancy = 0;
        this.numberOfTrades = 0;
    }
    
    /**
     * Gets the total return.
     * 
     * @return the total return
     */
    public double getTotalReturn() {
        return totalReturn;
    }
    
    /**
     * Sets the total return.
     * 
     * @param totalReturn the total return
     */
    public void setTotalReturn(double totalReturn) {
        this.totalReturn = totalReturn;
    }
    
    /**
     * Gets the maximum drawdown.
     * 
     * @return the maximum drawdown
     */
    public double getMaxDrawdown() {
        return maxDrawdown;
    }
    
    /**
     * Sets the maximum drawdown.
     * 
     * @param maxDrawdown the maximum drawdown
     */
    public void setMaxDrawdown(double maxDrawdown) {
        this.maxDrawdown = maxDrawdown;
    }
    
    /**
     * Gets the Sharpe ratio.
     * 
     * @return the Sharpe ratio
     */
    public double getSharpeRatio() {
        return sharpeRatio;
    }
    
    /**
     * Sets the Sharpe ratio.
     * 
     * @param sharpeRatio the Sharpe ratio
     */
    public void setSharpeRatio(double sharpeRatio) {
        this.sharpeRatio = sharpeRatio;
    }
    
    /**
     * Gets the volatility.
     * 
     * @return the volatility
     */
    public double getVolatility() {
        return volatility;
    }
    
    /**
     * Sets the volatility.
     * 
     * @param volatility the volatility
     */
    public void setVolatility(double volatility) {
        this.volatility = volatility;
    }
    
    /**
     * Gets the Sortino ratio.
     * 
     * @return the Sortino ratio
     */
    public double getSortinoRatio() {
        return sortinoRatio;
    }
    
    /**
     * Sets the Sortino ratio.
     * 
     * @param sortinoRatio the Sortino ratio
     */
    public void setSortinoRatio(double sortinoRatio) {
        this.sortinoRatio = sortinoRatio;
    }
    
    /**
     * Gets the Calmar ratio.
     * 
     * @return the Calmar ratio
     */
    public double getCalmarRatio() {
        return calmarRatio;
    }
    
    /**
     * Sets the Calmar ratio.
     * 
     * @param calmarRatio the Calmar ratio
     */
    public void setCalmarRatio(double calmarRatio) {
        this.calmarRatio = calmarRatio;
    }
    
    /**
     * Gets the win rate.
     * 
     * @return the win rate
     */
    public double getWinRate() {
        return winRate;
    }
    
    /**
     * Sets the win rate.
     * 
     * @param winRate the win rate
     */
    public void setWinRate(double winRate) {
        this.winRate = winRate;
    }
    
    /**
     * Gets the profit factor.
     * 
     * @return the profit factor
     */
    public double getProfitFactor() {
        return profitFactor;
    }
    
    /**
     * Sets the profit factor.
     * 
     * @param profitFactor the profit factor
     */
    public void setProfitFactor(double profitFactor) {
        this.profitFactor = profitFactor;
    }
    
    /**
     * Gets the expectancy.
     * 
     * @return the expectancy
     */
    public double getExpectancy() {
        return expectancy;
    }
    
    /**
     * Sets the expectancy.
     * 
     * @param expectancy the expectancy
     */
    public void setExpectancy(double expectancy) {
        this.expectancy = expectancy;
    }
    
    /**
     * Gets the number of trades.
     * 
     * @return the number of trades
     */
    public int getNumberOfTrades() {
        return numberOfTrades;
    }
    
    /**
     * Sets the number of trades.
     * 
     * @param numberOfTrades the number of trades
     */
    public void setNumberOfTrades(int numberOfTrades) {
        this.numberOfTrades = numberOfTrades;
    }
    
    @Override
    public String toString() {
        return "RiskMetrics{" +
                "totalReturn=" + String.format("%.2f%%", totalReturn * 100) +
                ", maxDrawdown=" + String.format("%.2f%%", maxDrawdown * 100) +
                ", sharpeRatio=" + String.format("%.2f", sharpeRatio) +
                ", volatility=" + String.format("%.2f%%", volatility * 100) +
                ", sortinoRatio=" + String.format("%.2f", sortinoRatio) +
                ", calmarRatio=" + String.format("%.2f", calmarRatio) +
                ", winRate=" + String.format("%.2f%%", winRate * 100) +
                ", profitFactor=" + String.format("%.2f", profitFactor) +
                ", expectancy=$" + String.format("%.2f", expectancy) +
                ", numberOfTrades=" + numberOfTrades +
                '}';
    }
}
