package com.tradingbacktester.ui;

import javafx.scene.layout.BorderPane;

/**
 * Panel for configuring trading strategies.
 */
public class StrategyConfigPanel extends BorderPane {
    
    private final TradingBacktesterApp app;
    
    /**
     * Creates a new strategy configuration panel.
     * 
     * @param app the main application
     */
    public StrategyConfigPanel(TradingBacktesterApp app) {
        this.app = app;
        
        // Create UI
        BacktestUI backtestUI = new BacktestUI(app);
        setCenter(backtestUI.createBacktestPanel());
    }
}
