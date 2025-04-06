package com.tradingbacktester.ui;

import com.tradingbacktester.core.Trade;
import com.tradingbacktester.utils.Constants;
import com.tradingbacktester.utils.MathUtils;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Panel for displaying performance metrics and charts.
 */
public class PerformancePanel extends BorderPane {
    
    private final TradingBacktesterApp app;
    
    /**
     * Creates a new performance panel.
     * 
     * @param app the main application
     */
    public PerformancePanel(TradingBacktesterApp app) {
        this.app = app;
        
        // Create the panel
        createPanel();
    }
    
    /**
     * Creates the panel content.
     */
    private void createPanel() {
        TabPane tabPane = new TabPane();
        
        // Summary tab
        Tab summaryTab = new Tab("Summary");
        summaryTab.setClosable(false);
        summaryTab.setContent(createSummaryPanel());
        
        // Equity curve tab
        Tab equityTab = new Tab("Equity Curve");
        equityTab.setClosable(false);
        equityTab.setContent(createEquityCurvePanel());
        
        // Monthly returns tab
        Tab monthlyTab = new Tab("Monthly Returns");
        monthlyTab.setClosable(false);
        monthlyTab.setContent(createMonthlyReturnsPanel());
        
        // Trade statistics tab
        Tab tradeStatsTab = new Tab("Trade Statistics");
        tradeStatsTab.setClosable(false);
        tradeStatsTab.setContent(createTradeStatsPanel());
        
        tabPane.getTabs().addAll(summaryTab, equityTab, monthlyTab, tradeStatsTab);
        
        setCenter(tabPane);
    }
    
    /**
     * Creates the summary panel.
     * 
     * @return the summary panel
     */
    private VBox createSummaryPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20));
        
        Map<String, Object> results = app.getBacktestResults();
        if (results == null) {
            Label noDataLabel = new Label("No backtest results available. Run a backtest first.");
            panel.getChildren().add(noDataLabel);
            return panel;
        }
        
        // Title
        Label titleLabel = new Label("Performance Summary");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        
        // Key metrics
        GridPane metricsGrid = new GridPane();
        metricsGrid.setHgap(20);
        metricsGrid.setVgap(10);
        
        int row = 0;
        
        // Add summary fields
        addMetricRow(metricsGrid, "Net Profit:", formatCurrency((Double) results.get("profit")), row++);
        addMetricRow(metricsGrid, "Return:", formatPercent((Double) results.get("returnPct")), row++);
        addMetricRow(metricsGrid, "Total Trades:", results.get("totalTrades").toString(), row++);
        
        int winningTrades = (Integer) results.get("winningTrades");
        int losingTrades = (Integer) results.get("losingTrades");
        
        addMetricRow(metricsGrid, "Winning Trades:", winningTrades + " (" + formatPercent((Double) results.get("winRate")) + ")", row++);
        addMetricRow(metricsGrid, "Losing Trades:", losingTrades + " (" + formatPercent(100.0 - (Double) results.get("winRate")) + ")", row++);
        
        addMetricRow(metricsGrid, "Average Profit:", formatCurrency((Double) results.get("averageProfit")), row++);
        addMetricRow(metricsGrid, "Average Loss:", formatCurrency((Double) results.get("averageLoss")), row++);
        
        double profitFactor = (Double) results.get("profitFactor");
        addMetricRow(metricsGrid, "Profit Factor:", String.format("%.2f", profitFactor), row++);
        
        double maxDrawdown = (Double) results.get("maxDrawdown");
        addMetricRow(metricsGrid, "Max Drawdown:", formatPercent(maxDrawdown), row++);
        
        // Add Sharpe ratio if available
        if (results.containsKey("sharpeRatio")) {
            double sharpeRatio = (Double) results.get("sharpeRatio");
            addMetricRow(metricsGrid, "Sharpe Ratio:", String.format("%.2f", sharpeRatio), row++);
        }
        
        // Add Sortino ratio if available
        if (results.containsKey("sortinoRatio")) {
            double sortinoRatio = (Double) results.get("sortinoRatio");
            addMetricRow(metricsGrid, "Sortino Ratio:", String.format("%.2f", sortinoRatio), row++);
        }
        
        // Add Calmar ratio if available
        if (results.containsKey("calmarRatio")) {
            double calmarRatio = (Double) results.get("calmarRatio");
            addMetricRow(metricsGrid, "Calmar Ratio:", String.format("%.2f", calmarRatio), row++);
        }
        
        // Add expectancy if available
        if (results.containsKey("expectancy")) {
            double expectancy = (Double) results.get("expectancy");
            addMetricRow(metricsGrid, "Expectancy:", formatCurrency(expectancy), row++);
        }
        
        // Add win/loss pie chart
        PieChart winLossChart = createWinLossPieChart(winningTrades, losingTrades);
        
        // Add all components to the panel
        panel.getChildren().addAll(titleLabel, metricsGrid, winLossChart);
        
        return panel;
    }
    
    /**
     * Adds a metric row to the grid.
     * 
     * @param grid the grid
     * @param labelText the label text
     * @param valueText the value text
     * @param row the row index
     */
    private void addMetricRow(GridPane grid, String labelText, String valueText, int row) {
        Label label = new Label(labelText);
        label.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        Label value = new Label(valueText);
        value.setFont(Font.font("System", 14));
        
        grid.add(label, 0, row);
        grid.add(value, 1, row);
    }
    
    /**
     * Creates a win/loss pie chart.
     * 
     * @param winningTrades the number of winning trades
     * @param losingTrades the number of losing trades
     * @return the pie chart
     */
    private PieChart createWinLossPieChart(int winningTrades, int losingTrades) {
        PieChart chart = new PieChart();
        chart.setTitle("Win/Loss Distribution");
        
        PieChart.Data winData = new PieChart.Data("Winning Trades", winningTrades);
        PieChart.Data lossData = new PieChart.Data("Losing Trades", losingTrades);
        
        chart.getData().add(winData);
        chart.getData().add(lossData);
        
        // Set colors
        winData.getNode().setStyle("-fx-pie-color: " + Constants.PROFIT_COLOR.replace("#", "") + ";");
        lossData.getNode().setStyle("-fx-pie-color: " + Constants.LOSS_COLOR.replace("#", "") + ";");
        
        return chart;
    }
    
    /**
     * Creates the equity curve panel.
     * 
     * @return the equity curve panel
     */
    private VBox createEquityCurvePanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20));
        
        List<Trade> trades = app.getTrades();
        if (trades == null || trades.isEmpty()) {
            Label noDataLabel = new Label("No trades available. Run a backtest first.");
            panel.getChildren().add(noDataLabel);
            return panel;
        }
        
        // Title
        Label titleLabel = new Label("Equity Curve");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        
        // Create equity curve chart
        LineChart<String, Number> equityChart = createEquityCurveChart(trades);
        
        // Create drawdown chart
        LineChart<String, Number> drawdownChart = createDrawdownChart(trades);
        
        // Add all components to the panel
        panel.getChildren().addAll(titleLabel, equityChart, drawdownChart);
        
        return panel;
    }
    
    /**
     * Creates an equity curve chart.
     * 
     * @param trades the trades
     * @return the equity curve chart
     */
    private LineChart<String, Number> createEquityCurveChart(List<Trade> trades) {
        // Sort trades by exit time
        List<Trade> sortedTrades = new ArrayList<>(trades);
        sortedTrades.sort(Comparator.comparing(Trade::getExitTime));
        
        // Create axes
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Date");
        yAxis.setLabel("Equity");
        
        // Create chart
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Equity Curve");
        
        // Create series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Equity");
        
        // Initial capital
        double initialCapital = app.getBacktestResults() != null ? 
            (Double) app.getBacktestResults().get("initialCapital") : 
            100000.0;
        
        // Add data points
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        
        series.getData().add(new XYChart.Data<>(
            sortedTrades.get(0).getEntryTime().format(formatter),
            initialCapital
        ));
        
        double equity = initialCapital;
        for (Trade trade : sortedTrades) {
            equity += trade.getProfit();
            
            series.getData().add(new XYChart.Data<>(
                trade.getExitTime().format(formatter),
                equity
            ));
        }
        
        chart.getData().add(series);
        
        return chart;
    }
    
    /**
     * Creates a drawdown chart.
     * 
     * @param trades the trades
     * @return the drawdown chart
     */
    private LineChart<String, Number> createDrawdownChart(List<Trade> trades) {
        // Sort trades by exit time
        List<Trade> sortedTrades = new ArrayList<>(trades);
        sortedTrades.sort(Comparator.comparing(Trade::getExitTime));
        
        // Create axes
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Date");
        yAxis.setLabel("Drawdown %");
        
        // Create chart
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Drawdown");
        
        // Create series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Drawdown");
        
        // Initial capital
        double initialCapital = app.getBacktestResults() != null ? 
            (Double) app.getBacktestResults().get("initialCapital") : 
            100000.0;
        
        // Calculate drawdown
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        double equity = initialCapital;
        double maxEquity = initialCapital;
        
        series.getData().add(new XYChart.Data<>(
            sortedTrades.get(0).getEntryTime().format(formatter),
            0.0
        ));
        
        for (Trade trade : sortedTrades) {
            equity += trade.getProfit();
            maxEquity = Math.max(maxEquity, equity);
            
            double drawdown = (maxEquity - equity) / maxEquity * 100;
            
            series.getData().add(new XYChart.Data<>(
                trade.getExitTime().format(formatter),
                drawdown
            ));
        }
        
        chart.getData().add(series);
        
        // Invert y-axis (drawdown is negative)
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        double maxDrawdown = app.getBacktestResults() != null ? 
            (Double) app.getBacktestResults().get("maxDrawdown") : 
            0.0;
        yAxis.setUpperBound(Math.ceil(maxDrawdown * 1.2));
        
        return chart;
    }
    
    /**
     * Creates the monthly returns panel.
     * 
     * @return the monthly returns panel
     */
    private VBox createMonthlyReturnsPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20));
        
        List<Trade> trades = app.getTrades();
        if (trades == null || trades.isEmpty()) {
            Label noDataLabel = new Label("No trades available. Run a backtest first.");
            panel.getChildren().add(noDataLabel);
            return panel;
        }
        
        // Title
        Label titleLabel = new Label("Monthly Returns");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        
        // Create monthly returns table
        GridPane monthlyGrid = createMonthlyReturnsGrid(trades);
        
        // Create monthly returns chart
        BarChart<String, Number> monthlyChart = createMonthlyReturnsChart(trades);
        
        // Add all components to the panel
        panel.getChildren().addAll(titleLabel, monthlyGrid, monthlyChart);
        
        return panel;
    }
    
    /**
     * Creates a monthly returns grid.
     * 
     * @param trades the trades
     * @return the monthly returns grid
     */
    private GridPane createMonthlyReturnsGrid(List<Trade> trades) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);
        grid.setPadding(new Insets(10));
        
        // Calculate monthly returns
        Map<String, Double> monthlyReturns = calculateMonthlyReturns(trades);
        
        // Create header row
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        
        for (int i = 0; i < months.length; i++) {
            Label monthLabel = new Label(months[i]);
            monthLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
            grid.add(monthLabel, i + 1, 0);
        }
        
        // Add year rows
        Set<Integer> years = new TreeSet<>();
        for (String key : monthlyReturns.keySet()) {
            String[] parts = key.split("-");
            years.add(Integer.parseInt(parts[0]));
        }
        
        int row = 1;
        for (Integer year : years) {
            // Year label
            Label yearLabel = new Label(year.toString());
            yearLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
            grid.add(yearLabel, 0, row);
            
            // Monthly returns
            for (int month = 1; month <= 12; month++) {
                String key = year + "-" + String.format("%02d", month);
                Double returnPct = monthlyReturns.get(key);
                
                if (returnPct != null) {
                    Label returnLabel = new Label(formatPercent(returnPct));
                    returnLabel.setTextFill(returnPct >= 0 ? Color.GREEN : Color.RED);
                    grid.add(returnLabel, month, row);
                } else {
                    grid.add(new Label(""), month, row);
                }
            }
            
            row++;
        }
        
        return grid;
    }
    
    /**
     * Creates a monthly returns bar chart.
     * 
     * @param trades the trades
     * @return the monthly returns bar chart
     */
    private BarChart<String, Number> createMonthlyReturnsChart(List<Trade> trades) {
        // Calculate monthly returns
        Map<String, Double> monthlyReturns = calculateMonthlyReturns(trades);
        
        // Create axes
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Month");
        yAxis.setLabel("Return (%)");
        
        // Create chart
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Monthly Returns");
        
        // Create series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Return");
        
        // Get sorted keys (year-month)
        List<String> sortedKeys = new ArrayList<>(monthlyReturns.keySet());
        sortedKeys.sort(String::compareTo);
        
        // Display names for months (e.g., "Jan 2023")
        Map<String, String> displayNames = new HashMap<>();
        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        
        for (String key : sortedKeys) {
            String[] parts = key.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            
            displayNames.put(key, monthNames[month - 1] + " " + year);
        }
        
        // Add data
        for (String key : sortedKeys) {
            Double returnPct = monthlyReturns.get(key);
            series.getData().add(new XYChart.Data<>(displayNames.get(key), returnPct));
        }
        
        chart.getData().add(series);
        
        // Color bars based on positive/negative returns
        for (XYChart.Data<String, Number> data : series.getData()) {
            double value = data.getYValue().doubleValue();
            if (value >= 0) {
                data.getNode().setStyle("-fx-bar-fill: " + Constants.PROFIT_COLOR + ";");
            } else {
                data.getNode().setStyle("-fx-bar-fill: " + Constants.LOSS_COLOR + ";");
            }
        }
        
        return chart;
    }
    
    /**
     * Calculates monthly returns from trades.
     * 
     * @param trades the trades
     * @return a map of year-month to return percentage
     */
    private Map<String, Double> calculateMonthlyReturns(List<Trade> trades) {
        Map<String, Double> monthlyProfits = new HashMap<>();
        
        // Initial capital
        double initialCapital = app.getBacktestResults() != null ? 
            (Double) app.getBacktestResults().get("initialCapital") : 
            100000.0;
        
        // Group trades by month
        for (Trade trade : trades) {
            LocalDate date = trade.getExitTime().toLocalDate();
            String key = date.getYear() + "-" + String.format("%02d", date.getMonthValue());
            
            monthlyProfits.put(key, monthlyProfits.getOrDefault(key, 0.0) + trade.getProfit());
        }
        
        // Calculate return percentages
        Map<String, Double> monthlyReturns = new HashMap<>();
        for (Map.Entry<String, Double> entry : monthlyProfits.entrySet()) {
            double returnPct = (entry.getValue() / initialCapital) * 100;
            monthlyReturns.put(entry.getKey(), returnPct);
        }
        
        return monthlyReturns;
    }
    
    /**
     * Creates the trade statistics panel.
     * 
     * @return the trade statistics panel
     */
    private VBox createTradeStatsPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20));
        
        List<Trade> trades = app.getTrades();
        if (trades == null || trades.isEmpty()) {
            Label noDataLabel = new Label("No trades available. Run a backtest first.");
            panel.getChildren().add(noDataLabel);
            return panel;
        }
        
        // Title
        Label titleLabel = new Label("Trade Statistics");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        
        // Create trade stats grid
        GridPane statsGrid = createTradeStatsGrid(trades);
        
        // Create profit distribution chart
        BarChart<String, Number> profitDistChart = createProfitDistributionChart(trades);
        
        // Create duration distribution chart
        BarChart<String, Number> durationDistChart = createDurationDistributionChart(trades);
        
        // Add all components to the panel
        panel.getChildren().addAll(titleLabel, statsGrid, profitDistChart, durationDistChart);
        
        return panel;
    }
    
    /**
     * Creates a trade statistics grid.
     * 
     * @param trades the trades
     * @return the trade statistics grid
     */
    private GridPane createTradeStatsGrid(List<Trade> trades) {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        
        int row = 0;
        
        // Total trades
        addMetricRow(grid, "Total Trades:", String.valueOf(trades.size()), row++);
        
        // Long/short trades
        int longTrades = 0;
        int shortTrades = 0;
        for (Trade trade : trades) {
            if (trade.isLong()) {
                longTrades++;
            } else {
                shortTrades++;
            }
        }
        
        addMetricRow(grid, "Long Trades:", longTrades + " (" + formatPercent(longTrades * 100.0 / trades.size()) + ")", row++);
        addMetricRow(grid, "Short Trades:", shortTrades + " (" + formatPercent(shortTrades * 100.0 / trades.size()) + ")", row++);
        
        // Winning/losing trades
        int winningTrades = 0;
        int losingTrades = 0;
        double totalProfit = 0;
        double totalLoss = 0;
        double maxProfit = 0;
        double maxLoss = 0;
        
        for (Trade trade : trades) {
            if (trade.getProfit() > 0) {
                winningTrades++;
                totalProfit += trade.getProfit();
                maxProfit = Math.max(maxProfit, trade.getProfit());
            } else {
                losingTrades++;
                totalLoss += Math.abs(trade.getProfit());
                maxLoss = Math.max(maxLoss, Math.abs(trade.getProfit()));
            }
        }
        
        double winRate = winningTrades * 100.0 / trades.size();
        
        addMetricRow(grid, "Winning Trades:", winningTrades + " (" + formatPercent(winRate) + ")", row++);
        addMetricRow(grid, "Losing Trades:", losingTrades + " (" + formatPercent(100 - winRate) + ")", row++);
        
        // Average profit/loss
        double avgProfit = winningTrades > 0 ? totalProfit / winningTrades : 0;
        double avgLoss = losingTrades > 0 ? totalLoss / losingTrades : 0;
        
        addMetricRow(grid, "Average Profit:", formatCurrency(avgProfit), row++);
        addMetricRow(grid, "Average Loss:", formatCurrency(avgLoss), row++);
        
        // Max profit/loss
        addMetricRow(grid, "Max Profit:", formatCurrency(maxProfit), row++);
        addMetricRow(grid, "Max Loss:", formatCurrency(maxLoss), row++);
        
        // Profit factor
        double profitFactor = totalLoss > 0 ? totalProfit / totalLoss : 0;
        addMetricRow(grid, "Profit Factor:", String.format("%.2f", profitFactor), row++);
        
        // Average trade
        double avgTrade = (totalProfit - totalLoss) / trades.size();
        addMetricRow(grid, "Average Trade:", formatCurrency(avgTrade), row++);
        
        // Trade duration
        double avgDuration = trades.stream()
            .mapToLong(Trade::getDurationDays)
            .average()
            .orElse(0);
        
        addMetricRow(grid, "Average Duration:", String.format("%.1f days", avgDuration), row++);
        
        long maxDuration = trades.stream()
            .mapToLong(Trade::getDurationDays)
            .max()
            .orElse(0);
        
        addMetricRow(grid, "Max Duration:", maxDuration + " days", row++);
        
        return grid;
    }
    
    /**
     * Creates a profit distribution chart.
     * 
     * @param trades the trades
     * @return the profit distribution chart
     */
    private BarChart<String, Number> createProfitDistributionChart(List<Trade> trades) {
        // Define profit bins
        List<Double> profits = trades.stream()
            .map(Trade::getProfit)
            .collect(Collectors.toList());
        
        double maxProfit = profits.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double minProfit = profits.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        
        // Create 10 bins
        int numBins = 10;
        double binSize = (maxProfit - minProfit) / numBins;
        
        // Count trades in each bin
        Map<Integer, Integer> binCounts = new HashMap<>();
        for (Double profit : profits) {
            int bin = (int) Math.floor((profit - minProfit) / binSize);
            if (bin == numBins) bin--; // Handle the case for max value
            binCounts.put(bin, binCounts.getOrDefault(bin, 0) + 1);
        }
        
        // Create axes
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Profit Range ($)");
        yAxis.setLabel("Number of Trades");
        
        // Create chart
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Profit Distribution");
        
        // Create series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Trades");
        
        // Add data
        for (int i = 0; i < numBins; i++) {
            double binStart = minProfit + i * binSize;
            double binEnd = minProfit + (i + 1) * binSize;
            
            String binLabel = String.format("$%.0f to $%.0f", binStart, binEnd);
            int count = binCounts.getOrDefault(i, 0);
            
            series.getData().add(new XYChart.Data<>(binLabel, count));
        }
        
        chart.getData().add(series);
        
        // Color bars based on profit/loss
        for (int i = 0; i < series.getData().size(); i++) {
            XYChart.Data<String, Number> data = series.getData().get(i);
            double binStart = minProfit + i * binSize;
            
            if (binStart >= 0) {
                data.getNode().setStyle("-fx-bar-fill: " + Constants.PROFIT_COLOR + ";");
            } else {
                data.getNode().setStyle("-fx-bar-fill: " + Constants.LOSS_COLOR + ";");
            }
        }
        
        return chart;
    }
    
    /**
     * Creates a duration distribution chart.
     * 
     * @param trades the trades
     * @return the duration distribution chart
     */
    private BarChart<String, Number> createDurationDistributionChart(List<Trade> trades) {
        // Define duration bins
        List<Long> durations = trades.stream()
            .map(Trade::getDurationDays)
            .collect(Collectors.toList());
        
        long maxDuration = durations.stream().mapToLong(Long::longValue).max().orElse(0);
        
        // Create bins for different durations
        String[] binLabels = {"1 day", "2-3 days", "4-7 days", "1-2 weeks", "2-4 weeks", "1-3 months", ">3 months"};
        long[] binThresholds = {1, 3, 7, 14, 28, 90, Long.MAX_VALUE};
        
        // Count trades in each bin
        int[] binCounts = new int[binLabels.length];
        
        for (Long duration : durations) {
            for (int i = 0; i < binThresholds.length; i++) {
                if (duration <= binThresholds[i]) {
                    binCounts[i]++;
                    break;
                }
            }
        }
        
        // Create axes
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Trade Duration");
        yAxis.setLabel("Number of Trades");
        
        // Create chart
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Trade Duration Distribution");
        
        // Create series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Trades");
        
        // Add data
        for (int i = 0; i < binLabels.length; i++) {
            series.getData().add(new XYChart.Data<>(binLabels[i], binCounts[i]));
        }
        
        chart.getData().add(series);
        
        return chart;
    }
    
    /**
     * Formats a currency value.
     * 
     * @param value the value
     * @return the formatted string
     */
    private String formatCurrency(double value) {
        return String.format("$%.2f", value);
    }
    
    /**
     * Formats a percentage value.
     * 
     * @param value the value
     * @return the formatted string
     */
    private String formatPercent(double value) {
        return String.format("%.2f%%", value);
    }
}
