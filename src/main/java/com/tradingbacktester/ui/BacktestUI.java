package com.tradingbacktester.ui;

import com.tradingbacktester.core.Trade;
import com.tradingbacktester.strategy.MovingAverageCrossover;
import com.tradingbacktester.strategy.RSIStrategy;
import com.tradingbacktester.strategy.Strategy;
import com.tradingbacktester.utils.Constants;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Main UI controller for the backtest interface.
 */
public class BacktestUI {
    
    private final TradingBacktesterApp app;
    
    // UI components
    private ComboBox<String> strategyTypeComboBox;
    private ComboBox<String> symbolComboBox;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private TextField initialCapitalField;
    private TextField commissionRateField;
    private TextField slippageModelField;
    private VBox strategyParametersBox;
    private Button runBacktestButton;
    private Label statusLabel;
    private TableView<Trade> tradesTable;
    private VBox resultsBox;
    
    // Strategy parameters
    private final Map<String, Map<String, Control>> strategyParameters = new HashMap<>();
    
    /**
     * Creates a new backtest UI controller.
     * 
     * @param app the main application
     */
    public BacktestUI(TradingBacktesterApp app) {
        this.app = app;
    }
    
    /**
     * Creates the UI components for the backtest panel.
     * 
     * @return the backtest panel
     */
    public VBox createBacktestPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        
        // Strategy selection
        HBox strategyBox = new HBox(10);
        strategyBox.setAlignment(Pos.CENTER_LEFT);
        
        Label strategyLabel = new Label("Strategy:");
        strategyTypeComboBox = new ComboBox<>();
        strategyTypeComboBox.getItems().addAll("Moving Average Crossover", "RSI Strategy");
        strategyTypeComboBox.setValue("Moving Average Crossover");
        strategyTypeComboBox.setOnAction(e -> updateStrategyParameters());
        
        strategyBox.getChildren().addAll(strategyLabel, strategyTypeComboBox);
        
        // Symbol selection
        HBox symbolBox = new HBox(10);
        symbolBox.setAlignment(Pos.CENTER_LEFT);
        
        Label symbolLabel = new Label("Symbol:");
        symbolComboBox = new ComboBox<>();
        symbolComboBox.getItems().addAll("AAPL", "MSFT", "GOOGL", "AMZN", "TSLA");
        symbolComboBox.setValue("AAPL");
        symbolComboBox.setEditable(true);
        
        Button loadDataButton = new Button("Load Data");
        loadDataButton.setOnAction(e -> loadMarketData());
        
        symbolBox.getChildren().addAll(symbolLabel, symbolComboBox, loadDataButton);
        
        // Date range
        HBox dateBox = new HBox(10);
        dateBox.setAlignment(Pos.CENTER_LEFT);
        
        Label startDateLabel = new Label("Start Date:");
        startDatePicker = new DatePicker(LocalDate.now().minusYears(1));
        
        Label endDateLabel = new Label("End Date:");
        endDatePicker = new DatePicker(LocalDate.now());
        
        dateBox.getChildren().addAll(startDateLabel, startDatePicker, endDateLabel, endDatePicker);
        
        // Capital and fees
        HBox capitalBox = new HBox(10);
        capitalBox.setAlignment(Pos.CENTER_LEFT);
        
        Label initialCapitalLabel = new Label("Initial Capital:");
        initialCapitalField = new TextField("100000");
        initialCapitalField.setPrefWidth(100);
        
        Label commissionLabel = new Label("Commission (%):");
        commissionRateField = new TextField("0.1");
        commissionRateField.setPrefWidth(60);
        
        Label slippageLabel = new Label("Slippage (%):");
        slippageModelField = new TextField("0.1");
        slippageModelField.setPrefWidth(60);
        
        capitalBox.getChildren().addAll(initialCapitalLabel, initialCapitalField, 
                                      commissionLabel, commissionRateField,
                                      slippageLabel, slippageModelField);
        
        // Strategy parameters
        TitledPane strategyParamPane = new TitledPane("Strategy Parameters", new VBox());
        strategyParamPane.setCollapsible(true);
        strategyParamPane.setExpanded(true);
        
        strategyParametersBox = new VBox(10);
        strategyParametersBox.setPadding(new Insets(10));
        strategyParamPane.setContent(strategyParametersBox);
        
        // Initialize parameter controls
        initializeParameterControls();
        updateStrategyParameters();
        
        // Run button and status
        HBox controlBox = new HBox(10);
        controlBox.setAlignment(Pos.CENTER);
        
        runBacktestButton = new Button("Run Backtest");
        runBacktestButton.setOnAction(e -> runBacktest());
        
        statusLabel = new Label("Ready");
        
        controlBox.getChildren().addAll(runBacktestButton, statusLabel);
        
        // Results section
        resultsBox = new VBox(10);
        resultsBox.setPadding(new Insets(10));
        resultsBox.setVisible(false);
        
        // Add all components to the panel
        panel.getChildren().addAll(
            strategyBox,
            symbolBox,
            dateBox,
            capitalBox,
            strategyParamPane,
            controlBox,
            resultsBox
        );
        
        return panel;
    }
    
    /**
     * Initializes the parameter controls for all strategy types.
     */
    private void initializeParameterControls() {
        // Moving Average Crossover parameters
        Map<String, Control> maParams = new HashMap<>();
        
        maParams.put("fastPeriod", createNumericTextField("12", 50));
        maParams.put("slowPeriod", createNumericTextField("26", 50));
        
        ComboBox<String> maTypeComboBox = new ComboBox<>();
        maTypeComboBox.getItems().addAll("SMA", "EMA");
        maTypeComboBox.setValue("SMA");
        maParams.put("useEMA", maTypeComboBox);
        
        maParams.put("positionSize", createPercentageTextField("10", 50));
        maParams.put("stopLossPercent", createPercentageTextField("5", 50));
        maParams.put("takeProfitPercent", createPercentageTextField("10", 50));
        
        strategyParameters.put("Moving Average Crossover", maParams);
        
        // RSI Strategy parameters
        Map<String, Control> rsiParams = new HashMap<>();
        
        rsiParams.put("period", createNumericTextField("14", 50));
        rsiParams.put("oversoldLevel", createNumericTextField("30", 50));
        rsiParams.put("overboughtLevel", createNumericTextField("70", 50));
        rsiParams.put("positionSize", createPercentageTextField("10", 50));
        rsiParams.put("stopLossPercent", createPercentageTextField("5", 50));
        rsiParams.put("takeProfitPercent", createPercentageTextField("10", 50));
        
        strategyParameters.put("RSI Strategy", rsiParams);
    }
    
    /**
     * Creates a numeric text field with validation.
     * 
     * @param defaultValue the default value
     * @param width the width
     * @return the text field
     */
    private TextField createNumericTextField(String defaultValue, double width) {
        TextField textField = new TextField(defaultValue);
        textField.setPrefWidth(width);
        
        // Add validation - only allow numbers
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                textField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        
        return textField;
    }
    
    /**
     * Creates a percentage text field with validation.
     * 
     * @param defaultValue the default value
     * @param width the width
     * @return the text field
     */
    private TextField createPercentageTextField(String defaultValue, double width) {
        TextField textField = new TextField(defaultValue);
        textField.setPrefWidth(width);
        
        // Add validation - only allow numbers and decimal point
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*\\.?\\d*")) {
                textField.setText(oldValue);
            }
        });
        
        return textField;
    }
    
    /**
     * Updates the strategy parameters UI based on the selected strategy type.
     */
    private void updateStrategyParameters() {
        String strategyType = strategyTypeComboBox.getValue();
        Map<String, Control> params = strategyParameters.get(strategyType);
        
        strategyParametersBox.getChildren().clear();
        
        if (params != null) {
            if ("Moving Average Crossover".equals(strategyType)) {
                // Fast period
                HBox fastPeriodBox = new HBox(10);
                fastPeriodBox.setAlignment(Pos.CENTER_LEFT);
                fastPeriodBox.getChildren().addAll(
                    new Label("Fast Period:"),
                    params.get("fastPeriod")
                );
                
                // Slow period
                HBox slowPeriodBox = new HBox(10);
                slowPeriodBox.setAlignment(Pos.CENTER_LEFT);
                slowPeriodBox.getChildren().addAll(
                    new Label("Slow Period:"),
                    params.get("slowPeriod")
                );
                
                // MA type
                HBox maTypeBox = new HBox(10);
                maTypeBox.setAlignment(Pos.CENTER_LEFT);
                maTypeBox.getChildren().addAll(
                    new Label("MA Type:"),
                    params.get("useEMA")
                );
                
                // Position size
                HBox positionSizeBox = new HBox(10);
                positionSizeBox.setAlignment(Pos.CENTER_LEFT);
                positionSizeBox.getChildren().addAll(
                    new Label("Position Size (%):"),
                    params.get("positionSize")
                );
                
                // Stop loss and take profit
                HBox stopLossBox = new HBox(10);
                stopLossBox.setAlignment(Pos.CENTER_LEFT);
                stopLossBox.getChildren().addAll(
                    new Label("Stop Loss (%):"),
                    params.get("stopLossPercent"),
                    new Label("Take Profit (%):"),
                    params.get("takeProfitPercent")
                );
                
                strategyParametersBox.getChildren().addAll(
                    fastPeriodBox,
                    slowPeriodBox,
                    maTypeBox,
                    positionSizeBox,
                    stopLossBox
                );
                
            } else if ("RSI Strategy".equals(strategyType)) {
                // RSI period
                HBox periodBox = new HBox(10);
                periodBox.setAlignment(Pos.CENTER_LEFT);
                periodBox.getChildren().addAll(
                    new Label("RSI Period:"),
                    params.get("period")
                );
                
                // Oversold and overbought levels
                HBox levelsBox = new HBox(10);
                levelsBox.setAlignment(Pos.CENTER_LEFT);
                levelsBox.getChildren().addAll(
                    new Label("Oversold Level:"),
                    params.get("oversoldLevel"),
                    new Label("Overbought Level:"),
                    params.get("overboughtLevel")
                );
                
                // Position size
                HBox positionSizeBox = new HBox(10);
                positionSizeBox.setAlignment(Pos.CENTER_LEFT);
                positionSizeBox.getChildren().addAll(
                    new Label("Position Size (%):"),
                    params.get("positionSize")
                );
                
                // Stop loss and take profit
                HBox stopLossBox = new HBox(10);
                stopLossBox.setAlignment(Pos.CENTER_LEFT);
                stopLossBox.getChildren().addAll(
                    new Label("Stop Loss (%):"),
                    params.get("stopLossPercent"),
                    new Label("Take Profit (%):"),
                    params.get("takeProfitPercent")
                );
                
                strategyParametersBox.getChildren().addAll(
                    periodBox,
                    levelsBox,
                    positionSizeBox,
                    stopLossBox
                );
            }
        }
    }
    
    /**
     * Loads market data for the selected symbol.
     */
    private void loadMarketData() {
        String symbol = symbolComboBox.getValue();
        if (symbol == null || symbol.trim().isEmpty()) {
            showAlert("Error", "Please enter a valid symbol.");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Market Data");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            statusLabel.setText("Loading data for " + symbol + "...");
            
            // TODO: Implement actual data loading
            
            statusLabel.setText("Data for " + symbol + " loaded successfully.");
        }
    }
    
    /**
     * Runs the backtest with the current parameters.
     */
    private void runBacktest() {
        try {
            // Get parameters
            String strategyType = strategyTypeComboBox.getValue();
            String symbol = symbolComboBox.getValue();
            LocalDateTime startDate = startDatePicker.getValue().atStartOfDay();
            LocalDateTime endDate = endDatePicker.getValue().atTime(LocalTime.MAX);
            double initialCapital = Double.parseDouble(initialCapitalField.getText());
            double commissionRate = Double.parseDouble(commissionRateField.getText()) / 100.0;
            double slippageModel = Double.parseDouble(slippageModelField.getText()) / 100.0;
            
            // Create strategy
            Strategy strategy = createStrategy(strategyType, symbol);
            
            // Run backtest
            statusLabel.setText("Running backtest...");
            runBacktestButton.setDisable(true);
            
            // Run in a background thread
            new Thread(() -> {
                try {
                    Map<String, Object> results = app.runBacktest(
                        symbol, startDate, endDate,
                        Collections.singletonList(strategy),
                        initialCapital, commissionRate, slippageModel
                    );
                    
                    Platform.runLater(() -> {
                        displayResults(results);
                        statusLabel.setText("Backtest completed.");
                        runBacktestButton.setDisable(false);
                    });
                    
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        showAlert("Error", "Backtest failed: " + e.getMessage());
                        statusLabel.setText("Backtest failed.");
                        runBacktestButton.setDisable(false);
                    });
                }
            }).start();
            
        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid numeric value: " + e.getMessage());
        } catch (Exception e) {
            showAlert("Error", "Backtest failed: " + e.getMessage());
        }
    }
    
    /**
     * Creates a strategy based on the selected type and parameters.
     * 
     * @param strategyType the strategy type
     * @param symbol the symbol to trade
     * @return the strategy
     */
    private Strategy createStrategy(String strategyType, String symbol) {
        Map<String, Control> params = strategyParameters.get(strategyType);
        
        if ("Moving Average Crossover".equals(strategyType)) {
            int fastPeriod = Integer.parseInt(((TextField) params.get("fastPeriod")).getText());
            int slowPeriod = Integer.parseInt(((TextField) params.get("slowPeriod")).getText());
            boolean useEMA = "EMA".equals(((ComboBox<?>) params.get("useEMA")).getValue());
            double positionSize = Double.parseDouble(((TextField) params.get("positionSize")).getText()) / 100.0;
            double stopLossPercent = Double.parseDouble(((TextField) params.get("stopLossPercent")).getText());
            double takeProfitPercent = Double.parseDouble(((TextField) params.get("takeProfitPercent")).getText());
            
            return new MovingAverageCrossover(
                symbol, fastPeriod, slowPeriod, useEMA,
                positionSize, stopLossPercent, takeProfitPercent
            );
            
        } else if ("RSI Strategy".equals(strategyType)) {
            int period = Integer.parseInt(((TextField) params.get("period")).getText());
            double oversoldLevel = Double.parseDouble(((TextField) params.get("oversoldLevel")).getText());
            double overboughtLevel = Double.parseDouble(((TextField) params.get("overboughtLevel")).getText());
            double positionSize = Double.parseDouble(((TextField) params.get("positionSize")).getText()) / 100.0;
            double stopLossPercent = Double.parseDouble(((TextField) params.get("stopLossPercent")).getText());
            double takeProfitPercent = Double.parseDouble(((TextField) params.get("takeProfitPercent")).getText());
            
            return new RSIStrategy(
                symbol, period, oversoldLevel, overboughtLevel,
                positionSize, stopLossPercent, takeProfitPercent
            );
        }
        
        throw new IllegalArgumentException("Unknown strategy type: " + strategyType);
    }
    
    /**
     * Displays the backtest results.
     * 
     * @param results the backtest results
     */
    @SuppressWarnings("unchecked")
    private void displayResults(Map<String, Object> results) {
        resultsBox.getChildren().clear();
        resultsBox.setVisible(true);
        
        // Summary
        TitledPane summaryPane = new TitledPane("Summary", new VBox());
        summaryPane.setCollapsible(true);
        summaryPane.setExpanded(true);
        
        GridPane summaryGrid = new GridPane();
        summaryGrid.setHgap(10);
        summaryGrid.setVgap(5);
        summaryGrid.setPadding(new Insets(10));
        
        // Add summary fields
        int row = 0;
        
        summaryGrid.add(new Label("Initial Capital:"), 0, row);
        summaryGrid.add(new Label(String.format("$%.2f", results.get("initialCapital"))), 1, row++);
        
        summaryGrid.add(new Label("Final Capital:"), 0, row);
        summaryGrid.add(new Label(String.format("$%.2f", results.get("finalCapital"))), 1, row++);
        
        summaryGrid.add(new Label("Profit:"), 0, row);
        double profit = (Double) results.get("profit");
        Label profitLabel = new Label(String.format("$%.2f", profit));
        profitLabel.setStyle(profit >= 0 ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
        summaryGrid.add(profitLabel, 1, row++);
        
        summaryGrid.add(new Label("Return:"), 0, row);
        double returnPct = (Double) results.get("returnPct");
        Label returnLabel = new Label(String.format("%.2f%%", returnPct));
        returnLabel.setStyle(returnPct >= 0 ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
        summaryGrid.add(returnLabel, 1, row++);
        
        summaryGrid.add(new Label("Total Trades:"), 0, row);
        summaryGrid.add(new Label(results.get("totalTrades").toString()), 1, row++);
        
        summaryGrid.add(new Label("Winning Trades:"), 0, row);
        summaryGrid.add(new Label(results.get("winningTrades").toString()), 1, row++);
        
        summaryGrid.add(new Label("Losing Trades:"), 0, row);
        summaryGrid.add(new Label(results.get("losingTrades").toString()), 1, row++);
        
        summaryGrid.add(new Label("Win Rate:"), 0, row);
        summaryGrid.add(new Label(String.format("%.2f%%", results.get("winRate"))), 1, row++);
        
        summaryGrid.add(new Label("Max Drawdown:"), 0, row);
        summaryGrid.add(new Label(String.format("%.2f%%", results.get("maxDrawdown"))), 1, row++);
        
        summaryPane.setContent(summaryGrid);
        
        // Trades table
        TitledPane tradesPane = new TitledPane("Trades", new VBox());
        tradesPane.setCollapsible(true);
        tradesPane.setExpanded(false);
        
        tradesTable = new TableView<>();
        
        // Define columns
        TableColumn<Trade, String> symbolCol = new TableColumn<>("Symbol");
        symbolCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getSymbol()));
        
        TableColumn<Trade, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().isLong() ? "LONG" : "SHORT"));
        
        TableColumn<Trade, String> entryDateCol = new TableColumn<>("Entry Date");
        entryDateCol.setCellValueFactory(p -> new SimpleStringProperty(
            p.getValue().getEntryTime().format(DateTimeFormatter.ISO_LOCAL_DATE)
        ));
        
        TableColumn<Trade, String> exitDateCol = new TableColumn<>("Exit Date");
        exitDateCol.setCellValueFactory(p -> new SimpleStringProperty(
            p.getValue().getExitTime().format(DateTimeFormatter.ISO_LOCAL_DATE)
        ));
        
        TableColumn<Trade, String> entryPriceCol = new TableColumn<>("Entry Price");
        entryPriceCol.setCellValueFactory(p -> new SimpleStringProperty(
            String.format("%.2f", p.getValue().getEntryPrice())
        ));
        
        TableColumn<Trade, String> exitPriceCol = new TableColumn<>("Exit Price");
        exitPriceCol.setCellValueFactory(p -> new SimpleStringProperty(
            String.format("%.2f", p.getValue().getExitPrice())
        ));
        
        TableColumn<Trade, String> profitCol = new TableColumn<>("Profit");
        profitCol.setCellValueFactory(p -> new SimpleStringProperty(
            String.format("$%.2f", p.getValue().getProfit())
        ));
        
        TableColumn<Trade, String> profitPctCol = new TableColumn<>("Profit %");
        profitPctCol.setCellValueFactory(p -> new SimpleStringProperty(
            String.format("%.2f%%", p.getValue().getProfitPercent())
        ));
        
        TableColumn<Trade, String> durationCol = new TableColumn<>("Duration (days)");
        durationCol.setCellValueFactory(p -> new SimpleStringProperty(
            String.valueOf(p.getValue().getDurationDays())
        ));
        
        tradesTable.getColumns().addAll(
            symbolCol, typeCol, entryDateCol, exitDateCol,
            entryPriceCol, exitPriceCol, profitCol, profitPctCol, durationCol
        );
        
        // Add data
        List<Trade> trades = (List<Trade>) results.get("trades");
        ObservableList<Trade> tradeData = FXCollections.observableArrayList(trades);
        tradesTable.setItems(tradeData);
        
        VBox tradesBox = new VBox(10);
        tradesBox.setPadding(new Insets(10));
        tradesBox.getChildren().add(tradesTable);
        tradesPane.setContent(tradesBox);
        
        resultsBox.getChildren().addAll(summaryPane, tradesPane);
    }
    
    /**
     * Shows an alert dialog.
     * 
     * @param title the alert title
     * @param message the alert message
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
