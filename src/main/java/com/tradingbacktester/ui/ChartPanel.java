package com.tradingbacktester.ui;

import com.tradingbacktester.core.Trade;
import com.tradingbacktester.indicators.*;
import com.tradingbacktester.model.Bar;
import com.tradingbacktester.model.TimeSeries;
import com.tradingbacktester.utils.Constants;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Panel for displaying price charts and indicators.
 */
public class ChartPanel extends BorderPane {
    
    private final TradingBacktesterApp app;
    
    private ComboBox<String> indicatorComboBox;
    private ComboBox<String> periodComboBox;
    private VBox chartContainer;
    private CheckBox showTradesCheckBox;
    
    /**
     * Creates a new chart panel.
     * 
     * @param app the main application
     */
    public ChartPanel(TradingBacktesterApp app) {
        this.app = app;
        
        // Create controls
        HBox controlBox = createControlBox();
        
        // Create chart container
        chartContainer = new VBox();
        chartContainer.setPadding(new Insets(10));
        
        setTop(controlBox);
        setCenter(chartContainer);
        
        // Initial chart
        updateChart();
    }
    
    /**
     * Creates the control box for chart options.
     * 
     * @return the control box
     */
    private HBox createControlBox() {
        HBox controlBox = new HBox(10);
        controlBox.setPadding(new Insets(10));
        controlBox.setAlignment(Pos.CENTER_LEFT);
        
        // Indicator selection
        Label indicatorLabel = new Label("Indicator:");
        indicatorComboBox = new ComboBox<>();
        indicatorComboBox.getItems().addAll(
            "None", "SMA", "EMA", "Bollinger Bands", "RSI", "MACD"
        );
        indicatorComboBox.setValue("None");
        indicatorComboBox.setOnAction(e -> updateChart());
        
        // Period selection
        Label periodLabel = new Label("Period:");
        periodComboBox = new ComboBox<>();
        periodComboBox.getItems().addAll(
            "5", "10", "20", "50", "100", "200"
        );
        periodComboBox.setValue("20");
        periodComboBox.setOnAction(e -> updateChart());
        
        // Show trades checkbox
        showTradesCheckBox = new CheckBox("Show Trades");
        showTradesCheckBox.setSelected(true);
        showTradesCheckBox.setOnAction(e -> updateChart());
        
        // Update button
        Button updateButton = new Button("Update Chart");
        updateButton.setOnAction(e -> updateChart());
        
        controlBox.getChildren().addAll(
            indicatorLabel, indicatorComboBox,
            periodLabel, periodComboBox,
            showTradesCheckBox,
            updateButton
        );
        
        return controlBox;
    }
    
    /**
     * Updates the chart with the latest data and selected indicators.
     */
    private void updateChart() {
        chartContainer.getChildren().clear();
        
        Map<String, Object> results = app.getBacktestResults();
        if (results == null) {
            Label noDataLabel = new Label("No backtest results available. Run a backtest first.");
            chartContainer.getChildren().add(noDataLabel);
            return;
        }
        
        @SuppressWarnings("unchecked")
        List<Trade> trades = (List<Trade>) results.get("trades");
        if (trades == null || trades.isEmpty()) {
            Label noTradesLabel = new Label("No trades available.");
            chartContainer.getChildren().add(noTradesLabel);
            return;
        }
        
        // Get the symbol from the first trade
        String symbol = trades.get(0).getSymbol();
        
        // Create price chart
        XYChart priceChart = createPriceChart(symbol, trades);
        if (priceChart != null) {
            javafx.embed.swing.SwingNode priceChartNode = new javafx.embed.swing.SwingNode();
            priceChartNode.setContent(new XChartPanel<>(priceChart));
            
            chartContainer.getChildren().add(priceChartNode);
        } else {
            Label noDataLabel = new Label("No price data available for " + symbol);
            chartContainer.getChildren().add(noDataLabel);
        }
        
        // Create indicator chart if selected
        String selectedIndicator = indicatorComboBox.getValue();
        if (!"None".equals(selectedIndicator)) {
            XYChart indicatorChart = createIndicatorChart(symbol, trades, selectedIndicator);
            if (indicatorChart != null) {
                javafx.embed.swing.SwingNode indicatorChartNode = new javafx.embed.swing.SwingNode();
                indicatorChartNode.setContent(new XChartPanel<>(indicatorChart));
                
                chartContainer.getChildren().add(indicatorChartNode);
            }
        }
    }
    
    /**
     * Creates a price chart for the specified symbol and trades.
     * 
     * @param symbol the symbol
     * @param trades the trades
     * @return the price chart
     */
    private XYChart createPriceChart(String symbol, List<Trade> trades) {
        // Get market data
        TimeSeries series = null;
        try {
            // Get the date range from trades
            LocalDateTime startDate = trades.stream()
                .map(Trade::getEntryTime)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().minusYears(1));
            
            LocalDateTime endDate = trades.stream()
                .map(Trade::getExitTime)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
            
            // Add some padding to the date range
            startDate = startDate.minusDays(30);
            endDate = endDate.plusDays(30);
            
            series = app.getMarketDataService().getHistoricalData(symbol, startDate, endDate);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        
        if (series == null || series.getBarCount() == 0) {
            return null;
        }
        
        // Create chart
        XYChart chart = new XYChartBuilder()
            .width(800)
            .height(400)
            .title("Price Chart - " + symbol)
            .xAxisTitle("Date")
            .yAxisTitle("Price")
            .build();
        
        // Customize chart
        chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideE);
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        chart.getStyler().setMarkerSize(0);
        chart.getStyler().setAxisTitlesVisible(true);
        chart.getStyler().setChartBackgroundColor(Color.WHITE);
        chart.getStyler().setPlotBackgroundColor(Color.WHITE);
        chart.getStyler().setPlotBorderVisible(true);
        chart.getStyler().setPlotGridLinesVisible(true);
        chart.getStyler().setXAxisTickMarkSpacingHint(100);
        
        // Add price data
        List<Bar> bars = series.getBars();
        List<Date> dates = bars.stream()
            .map(bar -> Date.from(bar.getTimestamp().atZone(ZoneId.systemDefault()).toInstant()))
            .collect(Collectors.toList());
        
        List<Double> closePrices = bars.stream()
            .map(Bar::getClose)
            .collect(Collectors.toList());
        
        chart.addSeries("Price", dates, closePrices)
            .setLineColor(Color.decode(Constants.PRICE_COLOR))
            .setMarker(SeriesMarkers.NONE);
        
        // Add selected indicators
        addIndicatorsToChart(chart, series);
        
        // Add trades if selected
        if (showTradesCheckBox.isSelected()) {
            addTradesToChart(chart, trades, series);
        }
        
        return chart;
    }
    
    /**
     * Adds indicators to the price chart.
     * 
     * @param chart the chart
     * @param series the time series
     */
    private void addIndicatorsToChart(XYChart chart, TimeSeries series) {
        String selectedIndicator = indicatorComboBox.getValue();
        int period = Integer.parseInt(periodComboBox.getValue());
        
        List<Bar> bars = series.getBars();
        List<Date> dates = bars.stream()
            .map(bar -> Date.from(bar.getTimestamp().atZone(ZoneId.systemDefault()).toInstant()))
            .collect(Collectors.toList());
        
        if ("SMA".equals(selectedIndicator)) {
            // Add SMA
            SMA sma = new SMA(period);
            List<Double> smaValues = sma.calculate(series);
            
            chart.addSeries("SMA(" + period + ")", dates, smaValues)
                .setLineColor(Color.decode(Constants.MA_FAST_COLOR))
                .setMarker(SeriesMarkers.NONE);
                
        } else if ("EMA".equals(selectedIndicator)) {
            // Add EMA
            EMA ema = new EMA(period);
            List<Double> emaValues = ema.calculate(series);
            
            chart.addSeries("EMA(" + period + ")", dates, emaValues)
                .setLineColor(Color.decode(Constants.MA_FAST_COLOR))
                .setMarker(SeriesMarkers.NONE);
                
        } else if ("Bollinger Bands".equals(selectedIndicator)) {
            // Add Bollinger Bands
            BollingerBands bb = new BollingerBands(period, 2.0);
            List<List<Double>> bands = bb.calculateBands(series);
            
            chart.addSeries("Upper Band", dates, bands.get(0))
                .setLineColor(Color.decode(Constants.UPPER_BAND_COLOR))
                .setMarker(SeriesMarkers.NONE);
                
            chart.addSeries("Middle Band", dates, bands.get(1))
                .setLineColor(Color.decode(Constants.MIDDLE_BAND_COLOR))
                .setMarker(SeriesMarkers.NONE);
                
            chart.addSeries("Lower Band", dates, bands.get(2))
                .setLineColor(Color.decode(Constants.LOWER_BAND_COLOR))
                .setMarker(SeriesMarkers.NONE);
        }
    }
    
    /**
     * Adds trades to the price chart.
     * 
     * @param chart the chart
     * @param trades the trades
     * @param series the time series
     */
    private void addTradesToChart(XYChart chart, List<Trade> trades, TimeSeries series) {
        // Create maps for entry and exit points
        Map<Date, Double> buyPoints = new HashMap<>();
        Map<Date, Double> sellPoints = new HashMap<>();
        
        for (Trade trade : trades) {
            Date entryDate = Date.from(trade.getEntryTime().atZone(ZoneId.systemDefault()).toInstant());
            Date exitDate = Date.from(trade.getExitTime().atZone(ZoneId.systemDefault()).toInstant());
            
            if (trade.isLong()) {
                // Long trade: buy at entry, sell at exit
                buyPoints.put(entryDate, trade.getEntryPrice());
                sellPoints.put(exitDate, trade.getExitPrice());
            } else {
                // Short trade: sell at entry, buy at exit
                sellPoints.put(entryDate, trade.getEntryPrice());
                buyPoints.put(exitDate, trade.getExitPrice());
            }
        }
        
        // Add buy and sell markers
        if (!buyPoints.isEmpty()) {
            chart.addSeries("Buy", new ArrayList<>(buyPoints.keySet()), new ArrayList<>(buyPoints.values()))
                .setLineColor(Color.GREEN)
                .setMarkerColor(Color.GREEN)
                .setMarker(SeriesMarkers.CIRCLE)
                .setLineWidth(0.0f)
                .setShowInLegend(true);
        }
        
        if (!sellPoints.isEmpty()) {
            chart.addSeries("Sell", new ArrayList<>(sellPoints.keySet()), new ArrayList<>(sellPoints.values()))
                .setLineColor(Color.RED)
                .setMarkerColor(Color.RED)
                .setMarker(SeriesMarkers.CIRCLE)
                .setLineWidth(0.0f)
                .setShowInLegend(true);
        }
    }
    
    /**
     * Creates an indicator chart.
     * 
     * @param symbol the symbol
     * @param trades the trades
     * @param indicatorType the indicator type
     * @return the indicator chart
     */
    private XYChart createIndicatorChart(String symbol, List<Trade> trades, String indicatorType) {
        if ("RSI".equals(indicatorType) || "MACD".equals(indicatorType)) {
            // Get market data
            TimeSeries series = null;
            try {
                // Get the date range from trades
                LocalDateTime startDate = trades.stream()
                    .map(Trade::getEntryTime)
                    .min(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now().minusYears(1));
                
                LocalDateTime endDate = trades.stream()
                    .map(Trade::getExitTime)
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());
                
                // Add some padding to the date range
                startDate = startDate.minusDays(30);
                endDate = endDate.plusDays(30);
                
                series = app.getMarketDataService().getHistoricalData(symbol, startDate, endDate);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            
            if (series == null || series.getBarCount() == 0) {
                return null;
            }
            
            // Create indicator chart
            if ("RSI".equals(indicatorType)) {
                return createRSIChart(series);
            } else {
                return createMACDChart(series);
            }
        }
        
        return null;
    }
    
    /**
     * Creates an RSI chart.
     * 
     * @param series the time series
     * @return the RSI chart
     */
    private XYChart createRSIChart(TimeSeries series) {
        int period = Integer.parseInt(periodComboBox.getValue());
        
        // Create chart
        XYChart chart = new XYChartBuilder()
            .width(800)
            .height(200)
            .title("RSI(" + period + ")")
            .xAxisTitle("Date")
            .yAxisTitle("RSI")
            .build();
        
        // Customize chart
        chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideE);
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        chart.getStyler().setMarkerSize(0);
        chart.getStyler().setChartBackgroundColor(Color.WHITE);
        chart.getStyler().setPlotBackgroundColor(Color.WHITE);
        chart.getStyler().setPlotBorderVisible(true);
        chart.getStyler().setPlotGridLinesVisible(true);
        chart.getStyler().setYAxisMin(0.0);
        chart.getStyler().setYAxisMax(100.0);
        
        // Add RSI data
        RSI rsi = new RSI(period);
        List<Double> rsiValues = rsi.calculate(series);
        
        List<Bar> bars = series.getBars();
        List<Date> dates = bars.stream()
            .map(bar -> Date.from(bar.getTimestamp().atZone(ZoneId.systemDefault()).toInstant()))
            .collect(Collectors.toList());
        
        chart.addSeries("RSI", dates, rsiValues)
            .setLineColor(Color.decode(Constants.RSI_COLOR))
            .setMarker(SeriesMarkers.NONE);
        
        // Add overbought and oversold levels
        List<Double> overboughtLevel = dates.stream().map(d -> Constants.RSI_OVERBOUGHT_LEVEL).collect(Collectors.toList());
        List<Double> oversoldLevel = dates.stream().map(d -> Constants.RSI_OVERSOLD_LEVEL).collect(Collectors.toList());
        
        chart.addSeries("Overbought", dates, overboughtLevel)
            .setLineColor(Color.GRAY)
            .setLineStyle(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] {5.0f}, 0.0f))
            .setMarker(SeriesMarkers.NONE);
            
        chart.addSeries("Oversold", dates, oversoldLevel)
            .setLineColor(Color.GRAY)
            .setLineStyle(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] {5.0f}, 0.0f))
            .setMarker(SeriesMarkers.NONE);
        
        return chart;
    }
    
    /**
     * Creates a MACD chart.
     * 
     * @param series the time series
     * @return the MACD chart
     */
    private XYChart createMACDChart(TimeSeries series) {
        // Create chart
        XYChart chart = new XYChartBuilder()
            .width(800)
            .height(200)
            .title("MACD")
            .xAxisTitle("Date")
            .yAxisTitle("MACD")
            .build();
        
        // Customize chart
        chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideE);
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        chart.getStyler().setMarkerSize(0);
        chart.getStyler().setChartBackgroundColor(Color.WHITE);
        chart.getStyler().setPlotBackgroundColor(Color.WHITE);
        chart.getStyler().setPlotBorderVisible(true);
        chart.getStyler().setPlotGridLinesVisible(true);
        
        // Add MACD data
        MACD macd = new MACD();
        List<List<Double>> macdLines = macd.calculateLines(series);
        List<Double> macdLine = macdLines.get(0);
        List<Double> signalLine = macdLines.get(1);
        List<Double> histogram = macdLines.get(2);
        
        List<Bar> bars = series.getBars();
        List<Date> dates = bars.stream()
            .map(bar -> Date.from(bar.getTimestamp().atZone(ZoneId.systemDefault()).toInstant()))
            .collect(Collectors.toList());
        
        chart.addSeries("MACD Line", dates, macdLine)
            .setLineColor(Color.decode(Constants.MACD_LINE_COLOR))
            .setMarker(SeriesMarkers.NONE);
            
        chart.addSeries("Signal Line", dates, signalLine)
            .setLineColor(Color.decode(Constants.SIGNAL_LINE_COLOR))
            .setMarker(SeriesMarkers.NONE);
        
        // Add histogram
        for (int i = 0; i < histogram.size(); i++) {
            double value = histogram.get(i);
            if (!Double.isNaN(value)) {
                Color color = value >= 0 ? 
                    Color.decode(Constants.HISTOGRAM_POSITIVE_COLOR) : 
                    Color.decode(Constants.HISTOGRAM_NEGATIVE_COLOR);
                
                chart.addSeries("Hist " + i, 
                    List.of(dates.get(i), dates.get(i)), 
                    List.of(0.0, value))
                    .setLineColor(color)
                    .setLineWidth(3.0f)
                    .setShowInLegend(false);
            }
        }
        
        return chart;
    }
}
