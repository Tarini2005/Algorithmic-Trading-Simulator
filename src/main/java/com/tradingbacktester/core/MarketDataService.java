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
    
    public MarketDataService(MarketDataLoader dataLoader) {
        this.dataCache = new ConcurrentHashMap<>();
        this.dataLoader = dataLoader;
    }

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
                return filterTimeRange(cachedData, startTime, endTime);
            }
        }
        
        // Load data from source
        TimeSeries newData = dataLoader.loadData(symbol, startTime, endTime);
        
        dataCache.put(symbol, newData);
        
        return newData;
    }
    
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
    
    public void clearCache() {
        dataCache.clear();
    }
    
    public void removeFromCache(String symbol) {
        dataCache.remove(symbol);
    }
}
