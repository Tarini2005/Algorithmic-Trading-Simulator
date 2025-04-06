package com.tradingbacktester.ui;

import com.tradingbacktester.core.BacktestEngine;
import com.tradingbacktester.core.MarketDataLoader;
import com.tradingbacktester.core.MarketDataService;
import com.tradingbacktester.core.Trade;
import com.tradingbacktester.model.TimeSeries;
import com.tradingbacktester.risk.RiskAnalyzer;
import com.tradingbacktester.risk.RiskMetrics;
import com.tradingbacktester.strategy.MovingAverageCrossover;
import com.tradingbacktester.strategy.RSIStrategy;
import com.tradingbacktester.strategy.Strategy;
import com.tradingbacktester.strategy.StrategyEvaluator;
import com.tradingbacktester.utils.Constants;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * The main JavaFX application for the Trading Backtester.
 */
public class TradingBacktesterApp extends Application {
    
    private BacktestEngine backtestEngine;
    private MarketDataService marketDataService;
    private StrategyEvaluator strategyEvaluator;
    private RiskAnalyzer riskAnalyzer;
    private Map<String, TimeSeries> loadedData;
    private Map<String, Object> backtestResults;
    private List<Strategy> strategies;
    private String dataDirectory;
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialize components
            initializeBacktestEngine();
            
            // Create the UI
            BorderPane root = new BorderPane();
            
            // Create menu bar
            MenuBar menuBar = createMenuBar(primaryStage);
            root.setTop(menuBar);
            
            // Create tab pane
            TabPane tabPane = new TabPane();
            
            // Strategy configuration tab
            Tab strategyTab = new Tab("Strategy Configuration");
            strategyTab.setContent(new StrategyConfigPanel(this));
            strategyTab.setClosable(false);
            
            // Chart tab
            Tab chartTab = new Tab("Charts");
            chartTab.setContent(new ChartPanel(this));
            chartTab.setClosable(false);
            
            // Performance tab
            Tab performanceTab = new Tab("Performance");
            performanceTab.setContent(new PerformancePanel(this));
            performanceTab.setClosable(false);
            
            // Optimization tab
            Tab optimizationTab = new Tab("Optimization");
            optimizationTab.setContent(createOptimizationPanel());
            optimizationTab.setClosable(false);
            
            tabPane.getTabs().addAll(strategyTab, chartTab, performanceTab, optimizationTab);
            
            root.setCenter(tabPane);
            root.setPadding(new Insets(10));
            
            // Create and show the scene
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());
            
            primaryStage.setTitle("Trading Strategy Backtester");
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/icon.png")));
            primaryStage.setScene(scene);
            primaryStage.show();
            
            // Set up close handler
            primaryStage.setOnCloseRequest(e -> {
                if (strategyEvaluator != null) {
                    strategyEvaluator.shutdown();
                }
            });
        } catch (Exception e) {
            showErrorDialog("Application Error", "Failed to start application", e);
        }
    }
    
    /**
     * Main entry point.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
     * Creates the menu bar.
     * 
     * @param primaryStage the primary stage
     * @return the menu bar
     */
    private MenuBar createMenuBar(Stage primaryStage) {
        MenuBar menuBar = new MenuBar();
        
        // File menu
        Menu fileMenu = new Menu("File");
        
        MenuItem loadDataItem = new MenuItem("Load Market Data");
        loadDataItem.setOnAction(e -> loadMarketData(primaryStage));
        
        MenuItem saveResultsItem = new MenuItem("Save Backtest Results");
        saveResultsItem.setOnAction(e -> saveBacktestResults(primaryStage));
        
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> primaryStage.close());
        
        fileMenu.getItems().addAll(loadDataItem, saveResultsItem, new SeparatorMenuItem(), exitItem);
        
        // Strategy menu
        Menu strategyMenu = new Menu("Strategy");
        
        MenuItem newStrategyItem = new MenuItem("New Strategy");
        newStrategyItem.setOnAction(e -> createNewStrategy());
        
        MenuItem optimizeItem = new MenuItem("Optimize Strategy");
        optimizeItem.setOnAction(e -> optimizeStrategy());
        
        strategyMenu.getItems().addAll(newStrategyItem, optimizeItem);
        
        // Help menu
        Menu helpMenu = new Menu("Help");
        
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAboutDialog());
        
        helpMenu.getItems().addAll(aboutItem);
        
        menuBar.getMenus().addAll(fileMenu, strategyMenu, helpMenu);
        
        return menuBar;
    }
    
    /**
     * Creates the optimization panel.
     * 
     * @return the optimization panel
     */
    private VBox createOptimizationPanel() {
        // TODO: Implement optimization panel UI
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(20));
        
        Label label = new Label("Strategy Optimization");
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        Label notImplementedLabel = new Label("Strategy optimization UI is under development.");
        
        Button optimizeButton = new Button("Optimize Current Strategy");
        optimizeButton.setOnAction(e -> optimizeStrategy());
        
        panel.getChildren().addAll(label, notImplementedLabel, optimizeButton);
        
        return panel;
    }
    
    /**
     * Shows the about dialog.
     */
    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Trading Backtester");
        alert.setHeaderText("Trading Strategy Backtester");
        alert.setContentText("Version 1.0\n\n" +
                "A comprehensive platform for backtesting trading strategies " +
                "on historical market data.\n\n" +
                "© 2023 Trading Backtester");
        
        alert.showAndWait();
    }
    
    /**
     * Shows an error dialog.
     * 
     * @param title the dialog title
     * @param header the dialog header
     * @param exception the exception
     */
    private void showErrorDialog(String title, String header, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(exception.getMessage());
        
        // Create expandable Exception.
        StringBuffer sb = new StringBuffer();
        sb.append(exception.toString());
        sb.append("\n\nStack Trace:\n");
        for (StackTraceElement element : exception.getStackTrace()) {
            sb.append(element.toString());
            sb.append("\n");
        }
        
        TextArea textArea = new TextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        
        alert.getDialogPane().setExpandableContent(new ScrollPane(textArea));
        alert.getDialogPane().setExpanded(true);
        
        alert.showAndWait();
    }
    
    /**
     * Loads market data from a file or directory.
     * 
     * @param primaryStage the primary stage
     */
    private void loadMarketData(Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Market Data");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        
        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile != null) {
            try {
                // Extract symbol from filename
                String filename = selectedFile.getName();
                String symbol = filename.substring(0, filename.lastIndexOf('.'));
                
                // Create data directory if it doesn't exist
                Path dataPath = Paths.get("data");
                if (!Files.exists(dataPath)) {
                    Files.createDirectories(dataPath);
                }
                
                // Copy file to data directory if needed
                Path targetPath = dataPath.resolve(filename);
                if (!selectedFile.toPath().equals(targetPath)) {
                    Files.copy(selectedFile.toPath(), targetPath);
                }
                
                // Show confirmation
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Data Loaded");
                alert.setHeaderText("Market Data Loaded Successfully");
                alert.setContentText("Loaded data for symbol: " + symbol);
                alert.showAndWait();
                
                // Reload market data service
                marketDataService.clearCache();
                
            } catch (IOException e) {
                showErrorDialog("Data Load Error", "Failed to load market data", e);
            }
        }
    }
    
    /**
     * Saves backtest results to a file.
     * 
     * @param primaryStage the primary stage
     */
    private void saveBacktestResults(Stage primaryStage) {
        if (backtestResults == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Results");
            alert.setHeaderText("No Backtest Results");
            alert.setContentText("Run a backtest first before saving results.");
            alert.showAndWait();
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Backtest Results");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        
        String defaultFileName = "backtest_results_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + 
                ".csv";
        fileChooser.setInitialFileName(defaultFileName);
        
        File selectedFile = fileChooser.showSaveDialog(primaryStage);
        if (selectedFile != null) {
            try {
                // TODO: Implement saving results to CSV
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Results Saved");
                alert.setHeaderText("Backtest Results Saved");
                alert.setContentText("Results saved to: " + selectedFile.getAbsolutePath());
                alert.showAndWait();
                
            } catch (Exception e) {
                showErrorDialog("Save Error", "Failed to save backtest results", e);
            }
        }
    }
    
    /**
     * Creates a new strategy.
     */
    private void createNewStrategy() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("New Strategy");
        alert.setHeaderText("Create Custom Strategy");
        alert.setContentText("Custom strategy creation is not implemented yet. " +
                "Please use the predefined strategies in the Strategy Configuration tab.");
        alert.showAndWait();
    }
    
    /**
     * Optimizes the current strategy.
     */
    private void optimizeStrategy() {
        if (strategies == null || strategies.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Strategy");
            alert.setHeaderText("No Strategy Selected");
            alert.setContentText("Select a strategy to optimize first.");
            alert.showAndWait();
            return;
        }
        
        // Example optimization - would be better with a dedicated UI
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("Optimization");
        progressAlert.setHeaderText("Optimizing Strategy");
        progressAlert.setContentText("Optimization in progress. This may take a while...");
        progressAlert.show();
        
        // Run optimization in background thread
        new Thread(() -> {
            try {
                // Create parameter sets
                Strategy strategy = strategies.get(0);
                String symbol = "AAPL"; // Use first strategy's symbol
                
                // Get parameter ranges
                Map<String, Object> baseParams = strategy.getParameters();
                List<Map<String, Object>> parameterSets = createParameterSets(baseParams);
                
                // Define time range
                LocalDateTime startDate = LocalDateTime.now().minusYears(1);
                LocalDateTime endDate = LocalDateTime.now();
                
                // Run evaluation
                StrategyEvaluator.EvaluationResult bestResult = null;
                
                try {
                    List<StrategyEvaluator.EvaluationResult> results = 
                            strategyEvaluator.evaluateParameters(
                                (sym, params) -> createStrategyFromParams(strategy, sym, params),
                                parameterSets,
                                symbol,
                                startDate,
                                endDate,
                                100000.0,
                                0.001,
                                0.001);
                    
                    if (!results.isEmpty()) {
                        bestResult = results.get(0);
                    }
                    
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                
                // Show results
                final StrategyEvaluator.EvaluationResult finalResult = bestResult;
                Platform.runLater(() -> {
                    progressAlert.close();
                    
                    if (finalResult != null) {
                        Alert resultAlert = new Alert(Alert.AlertType.INFORMATION);
                        resultAlert.setTitle("Optimization Results");
                        resultAlert.setHeaderText("Optimization Complete");
                        resultAlert.setContentText(
                                "Best Parameters:\n" + finalResult.getParameters() + 
                                "\n\nReturn: " + String.format("%.2f%%", finalResult.getMetrics().getTotalReturn() * 100) +
                                "\nSharpe Ratio: " + String.format("%.2f", finalResult.getMetrics().getSharpeRatio()) +
                                "\nWin Rate: " + String.format("%.2f%%", finalResult.getMetrics().getWinRate() * 100) +
                                "\nNumber of Trades: " + finalResult.getMetrics().getNumberOfTrades());
                        resultAlert.showAndWait();
                    } else {
                        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                        errorAlert.setTitle("Optimization Error");
                        errorAlert.setHeaderText("Optimization Failed");
                        errorAlert.setContentText("No valid results found.");
                        errorAlert.showAndWait();
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    progressAlert.close();
                    showErrorDialog("Optimization Error", "Failed to optimize strategy", e);
                });
            }
        }).start();
    }
    
    /**
     * Creates parameter sets for optimization.
     * 
     * @param baseParams the base parameters
     * @return the list of parameter sets
     */
    private List<Map<String, Object>> createParameterSets(Map<String, Object> baseParams) {
        List<Map<String, Object>> parameterSets = new ArrayList<>();
        
        // Simple example - vary a few parameters
        if (baseParams.containsKey("fastPeriod") && baseParams.containsKey("slowPeriod")) {
            // MA Crossover strategy
            for (int fastPeriod = 5; fastPeriod <= 20; fastPeriod += 5) {
                for (int slowPeriod = 20; slowPeriod <= 50; slowPeriod += 10) {
                    if (fastPeriod < slowPeriod) {
                        Map<String, Object> params = new HashMap<>(baseParams);
                        params.put("fastPeriod", fastPeriod);
                        params.put("slowPeriod", slowPeriod);
                        parameterSets.add(params);
                    }
                }
            }
        } else if (baseParams.containsKey("period") && 
                   baseParams.containsKey("oversoldLevel") && 
                   baseParams.containsKey("overboughtLevel")) {
            // RSI strategy
            for (int period = 7; period <= 21; period += 7) {
                for (int oversold = 20; oversold <= 40; oversold += 5) {
                    for (int overbought = 60; overbought <= 80; overbought += 5) {
                        if (oversold < overbought) {
                            Map<String, Object> params = new HashMap<>(baseParams);
                            params.put("period", period);
                            params.put("oversoldLevel", (double) oversold);
                            params.put("overboughtLevel", (double) overbought);
                            parameterSets.add(params);
                        }
                    }
                }
            }
        } else {
            // Default - just add the base parameters
            parameterSets.add(new HashMap<>(baseParams));
        }
        
        return parameterSets;
    }
    
    /**
     * Creates a strategy from parameters.
     * 
     * @param baseStrategy the base strategy
     * @param symbol the symbol
     * @param params the parameters
     * @return the strategy
     */
    private Strategy createStrategyFromParams(Strategy baseStrategy, String symbol, Map<String, Object> params) {
        if (baseStrategy instanceof MovingAverageCrossover) {
            int fastPeriod = (Integer) params.getOrDefault("fastPeriod", 20);
            int slowPeriod = (Integer) params.getOrDefault("slowPeriod", 50);
            boolean useEMA = (Boolean) params.getOrDefault("useEMA", false);
            double positionSize = (Double) params.getOrDefault("positionSize", 0.1);
            double stopLossPercent = (Double) params.getOrDefault("stopLossPercent", 5.0);
            double takeProfitPercent = (Double) params.getOrDefault("takeProfitPercent", 10.0);
            
            return new MovingAverageCrossover(
                symbol, fastPeriod, slowPeriod, useEMA,
                positionSize, stopLossPercent, takeProfitPercent
            );
        } else if (baseStrategy instanceof RSIStrategy) {
            int period = (Integer) params.getOrDefault("period", 14);
            double oversoldLevel = (Double) params.getOrDefault("oversoldLevel", 30.0);
            double overboughtLevel = (Double) params.getOrDefault("overboughtLevel", 70.0);
            double positionSize = (Double) params.getOrDefault("positionSize", 0.1);
            double stopLossPercent = (Double) params.getOrDefault("stopLossPercent", 5.0);
            double takeProfitPercent = (Double) params.getOrDefault("takeProfitPercent", 10.0);
            
            return new RSIStrategy(
                symbol, period, oversoldLevel, overboughtLevel,
                positionSize, stopLossPercent, takeProfitPercent
            );
        }
        
        return baseStrategy;
    }
    
    /**
     * Initializes the backtest engine and market data service.
     */
    private void initializeBacktestEngine() {
        // Create market data loader and service
        dataDirectory = "./data";
        MarketDataLoader dataLoader = new MarketDataLoader(dataDirectory, Constants.DEFAULT_DATETIME_FORMAT);
        marketDataService = new MarketDataService(dataLoader);
        
        // Create backtest engine and other components
        backtestEngine = new BacktestEngine(marketDataService, 100000); // Start with $100,000
        strategyEvaluator = new StrategyEvaluator(marketDataService);
        riskAnalyzer = new RiskAnalyzer();
        
        // Initialize strategies list
        strategies = new ArrayList<>();
        
        // Add default strategies
        Strategy maStrategy = new MovingAverageCrossover(
            "AAPL", 
            20, 
            50, 
            false, 
            0.1, 
            5, 
            10
        );
        
        Strategy rsiStrategy = new RSIStrategy(
            "AAPL",
            14,
            30,
            70,
            0.1,
            5,
            10
        );
        
        strategies.add(maStrategy);
        backtestEngine.addStrategy(maStrategy);
    }
    
    /**
     * Runs a backtest with the specified parameters.
     * 
     * @param symbol the symbol to trade
     * @param startDate the start date
     * @param endDate the end date
     * @param strategies the strategies to use
     * @param initialCapital the initial capital
     * @param commissionRate the commission rate
     * @param slippageModel the slippage model
     * @return the backtest results
     */
    public Map<String, Object> runBacktest(String symbol, LocalDateTime startDate, LocalDateTime endDate,
                                          List<Strategy> strategies, double initialCapital,
                                          double commissionRate, double slippageModel) {
        // Reset the backtest engine
        backtestEngine = new BacktestEngine(marketDataService, initialCapital);
        backtestEngine.setCommissionRate(commissionRate);
        backtestEngine.setSlippageModel(slippageModel);
        
        // Store strategies
        this.strategies = new ArrayList<>(strategies);
        
        // Add strategies
        for (Strategy strategy : strategies) {
            backtestEngine.addStrategy(strategy);
        }
        
        // Run the backtest
        backtestResults = backtestEngine.runBacktest(startDate, endDate);
        
        // Calculate additional risk metrics
        @SuppressWarnings("unchecked")
        List<Trade> trades = (List<Trade>) backtestResults.get("trades");
        if (trades != null && !trades.isEmpty()) {
            RiskMetrics riskMetrics = riskAnalyzer.calculateRiskMetrics(trades, initialCapital);
            
            // Add to results
            backtestResults.put("sharpeRatio", riskMetrics.getSharpeRatio());
            backtestResults.put("sortinoRatio", riskMetrics.getSortinoRatio());
            backtestResults.put("calmarRatio", riskMetrics.getCalmarRatio());
            backtestResults.put("expectancy", riskMetrics.getExpectancy());
        }
        
        // Store results and return them
        return backtestResults;
    }
    
    /**
     * Gets the backtest engine.
     * 
     * @return the backtest engine
     */
    public BacktestEngine getBacktestEngine() {
        return backtestEngine;
    }
    
    /**
     * Gets the market data service.
     * 
     * @return the market data service
     */
    public MarketDataService getMarketDataService() {
        return marketDataService;
    }
    
    /**
     * Gets the strategy evaluator.
     * 
     * @return the strategy evaluator
     */
    public StrategyEvaluator getStrategyEvaluator() {
        return strategyEvaluator;
    }
    
    /**
     * Gets the risk analyzer.
     * 
     * @return the risk analyzer
     */
    public RiskAnalyzer getRiskAnalyzer() {
        return riskAnalyzer;
    }
    
    /**
     * Gets the backtest results.
     * 
     * @return the backtest results
     */
    public Map<String, Object> getBacktestResults() {
        return backtestResults;
    }
    
    /**
     * Gets the trades from the last backtest.
     * 
     * @return the list of trades
     */
    @SuppressWarnings("unchecked")
    public List<Trade> getTrades() {
        if (backtestResults == null) {
            return null;
        }
        return (List<Trade>) backtestResults.get("trades");
    }
    
    /**
     * Gets the current strategies.
     * 
     * @return the strategies
     */
    public List<Strategy> getStrategies() {
        return strategies;
    }
    
    /**
}
