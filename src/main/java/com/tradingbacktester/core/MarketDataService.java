package com.tradingbacktester.core;

import com.tradingbacktester.model.Bar;
import com.tradingbacktester.model.TimeSeries;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for accessing historical market data.
 */
public class MarketDataService {
    private final Map<String, TimeSeries> dataCache;
    private final MarketDataLoader dataLoader;
    
    /**
     * Creates a new market data service.
     * 
     * @param dataLoader the market data loader to use
     */
    public MarketDataService(MarketDataLoader dataLoader) {
        this.dataCache = new ConcurrentHashMap<>();
        this.dataLoader = dataLoader;
    }
    
    /**
     * Gets historical data for the specified symbol and time range.
     * 
     * @param symbol the symbol of the financial instrument
     * @param startTime the start time
     * @param endTime the end time
     * @return the time series containing the historical data
     */
    public TimeSeries getHistoricalData(String symbol, LocalDateTime startTime, LocalDateTime endTime) {
        // Check if data is in cache
        TimeSeries cachedData = dataCache.get(symbol);
        if (cachedData != null) {
            Bar firstBar = cachedData.getFirstBar();
            Bar lastBar = cachedData.getLastBar();
            
            // Check if the cached data covers the requested time range
            if (firstBar != null && lastBar != null && 
                !firstBar.getTimestamp().isAfter(startTime) && 
                !lastBar.getTimestamp().isBefore(endTime)) {
                // The cached data covers the requested time range
                return filterTimeRange(cachedData, startTime, endTime);
            }
        }
        
        // Load data from source
        TimeSeries newData = dataLoader.loadData(symbol, startTime, endTime);
        
        // Cache the data
        dataCache.put(symbol, newData);
        
        return newData;
    }
    
    /**
     * Filters a time series to include only data within the specified time range.
     * 
     * @param data the time series to filter
     * @param startTime the start time
     * @param endTime the end time
     * @return a new time series containing only data within the specified time range
     */
    private TimeSeries filterTimeRange(TimeSeries data, LocalDateTime startTime, LocalDateTime endTime) {
        List<Bar> bars = data.getBars();
        TimeSeries filteredData = new TimeSeries(data.getSymbol());
        
        for (Bar bar : bars) {
            if (!bar.getTimestamp().isBefore(startTime) && !bar.getTimestamp().isAfter(endTime)) {
                filteredData.addBar(bar);
            }
        }
        
        return filteredData;
    }
    
    /**
     * Clears the data cache.
     */
    public void clearCache() {
        dataCache.clear();
    }
    
    /**
     * Removes the specified symbol from the data cache.
     * 
     * @param symbol the symbol to remove
     */
    public void removeFromCache(String symbol) {
        dataCache.remove(symbol);
    }
}
