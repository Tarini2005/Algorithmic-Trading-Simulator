# Trading Strategy Backtester

A comprehensive Java-based platform for backtesting trading strategies on historical market data. This tool allows traders and developers to test and optimize trading algorithms with accurate modeling of market mechanics and trading costs.

## Features

- Robust backtesting engine with support for various order types
- Multiple built-in technical indicators (SMA, EMA, MACD, RSI, Bollinger Bands)
- Pre-built trading strategies (Moving Average Crossover, RSI)
- Realistic trading simulation with commission and slippage modeling
- Position sizing and risk management tools
- Detailed performance analysis and visualization
- Customizable parameter settings for strategy optimization

## Technologies

- Java 11+
- JavaFX for UI components
- Apache Commons Math for statistical calculations
- XChart for data visualization
- Maven for dependency management

## Project Structure

The project is organized into the following main packages:

- `com.tradingbacktester.core`: Core backtesting engine and components
- `com.tradingbacktester.indicators`: Technical indicators
- `com.tradingbacktester.model`: Data model classes
- `com.tradingbacktester.strategy`: Trading strategies
- `com.tradingbacktester.risk`: Risk analysis tools
- `com.tradingbacktester.ui`: User interface components
- `com.tradingbacktester.utils`: Utility classes

## Getting Started

### Prerequisites

- Java Development Kit (JDK) 11 or higher
- Apache Maven
- (Optional) An IDE like IntelliJ IDEA or Eclipse

### Installation

1. Clone this repository to your local machine:

```bash
git clone https://github.com/Tarini2005/trading-backtester.git
cd trading-backtester
```

2. Build the project using Maven:

```bash
mvn clean install
```

3. Run the application:

```bash
mvn javafx:run
```

Or using the JAR file:

```bash
java -jar target/trading-backtester-1.0-SNAPSHOT.jar
```

## Usage

### Loading Market Data

1. Create a CSV file with historical market data. The file should have the following columns:
   - datetime: Date and time in format yyyy-MM-dd HH:mm:ss
   - open: Opening price
   - high: Highest price
   - low: Lowest price
   - close: Closing price
   - volume: Trading volume (optional)

2. Place the CSV file in the `data` directory with a filename matching the symbol (e.g., `AAPL.csv` for Apple stock).

3. In the application, select the symbol and date range for your backtest.

### Running a Backtest

1. Select a trading strategy (Moving Average Crossover or RSI Strategy).
2. Configure the strategy parameters:
   - For Moving Average Crossover: Fast period, slow period, MA type
   - For RSI Strategy: RSI period, oversold and overbought levels
3. Set the position size, stop-loss, and take-profit parameters.
4. Adjust commission rate and slippage model as needed.
5. Click "Run Backtest" to start the simulation.

### Analyzing Results

After running a backtest, you can analyze the results in several ways:

1. **Summary**: Key performance metrics like total return, win rate, profit factor, and max drawdown
2. **Chart**: Price chart with indicators and trade entries/exits
3. **Performance**: Detailed performance analysis, equity curve, and monthly returns
4. **Trade Statistics**: Statistics about individual trades

## Extending the Platform

### Creating Custom Indicators

To add a new technical indicator, create a class that implements the `Indicator` interface:

```java
public class MyIndicator implements Indicator {
    @Override
    public List<Double> calculate(TimeSeries series) {
        // Implementation
    }
    
    @Override
    public String getName() {
        return "MyIndicator";
    }
}
```

### Creating Custom Strategies

To create a custom trading strategy, implement the `Strategy` interface:

```java
public class MyStrategy implements Strategy {
    @Override
    public String getName() {
        return "My Strategy";
    }
    
    @Override
    public Set<String> getRequiredSymbols() {
        // Return symbols needed by this strategy
    }
    
    @Override
    public void initialize(Map<String, TimeSeries> data) {
        // Initialize indicators and state
    }
    
    @Override
    public void onBar(LocalDateTime timestamp, Map<String, Bar> currentBars, Portfolio portfolio) {
        // Update strategy state on new bar
    }
    
    @Override
    public List<Order> generateOrders(LocalDateTime timestamp, Map<String, Bar> currentBars, Portfolio portfolio) {
        // Generate trading signals
    }
    
    @Override
    public Map<String, Object> getParameters() {
        // Return strategy parameters
    }
    
    @Override
    public void setParameter(String name, Object value) {
        // Set a strategy parameter
    }
}
```

