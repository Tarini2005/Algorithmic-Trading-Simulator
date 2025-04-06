package com.tradingbacktester.core;

import com.tradingbacktester.model.Bar;
import com.tradingbacktester.model.TimeSeries;
import com.tradingbacktester.utils.CsvUtils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads market data from various sources such as CSV files or APIs.
 */
public class MarketDataLoader {
    private String dataDirectory;
    private DateTimeFormatter dateTimeFormatter;
    
    /**
     * Creates a new market data loader with the default data directory and date format.
     */
    public MarketDataLoader() {
        this("./data", "yyyy-MM-dd HH:mm:ss");
    }
    
    /**
     * Creates a new market data loader with the specified data directory and date format.
     * 
     * @param dataDirectory the directory containing market data files
     * @param dateTimeFormat the date/time format used in the data files
     */
    public MarketDataLoader(String dataDirectory, String dateTimeFormat) {
        this.dataDirectory = dataDirectory;
        this.dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormat);
    }
    
    /**
     * Sets the data directory.
     * 
     * @param dataDirectory the directory containing market data files
     */
    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }
    
    /**
     * Sets the date/time format.
     * 
     * @param dateTimeFormat the date/time format used in the data files
     */
    public void setDateTimeFormat(String dateTimeFormat) {
        this.dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormat);
    }
    
    /**
     * Loads market data for the specified symbol and time range.
     * 
     * @param symbol the symbol of the financial instrument
     * @param startTime the start time
     * @param endTime the end time
     * @return the time series containing the historical data
     */
    public TimeSeries loadData(String symbol, LocalDateTime startTime, LocalDateTime endTime) {
        // First, try to load from a CSV file
        TimeSeries data = loadFromCsv(symbol);
        
        // If data is null, we could try to load from an API or other sources
        if (data == null) {
            throw new RuntimeException("Could not load data for symbol: " + symbol);
        }
        
        return filterTimeRange(data, startTime, endTime);
    }
    
    /**
     * Loads market data from a CSV file.
     * 
     * @param symbol the symbol of the financial instrument
     * @return the time series containing the historical data, or null if not found
     */
    private TimeSeries loadFromCsv(String symbol) {
        File file = new File(dataDirectory + File.separator + symbol + ".csv");
        if (!file.exists()) {
            return null;
        }
        
        try {
            List<String[]> rows = CsvUtils.readCsv(file.getPath());
            if (rows.isEmpty()) {
                return null;
            }
            
            // Determine the column indices (assuming the first row is the header)
            String[] header = rows.get(0);
            int dateTimeIdx = findColumnIndex(header, "datetime", "date", "time");
            int openIdx = findColumnIndex(header, "open");
            int highIdx = findColumnIndex(header, "high");
            int lowIdx = findColumnIndex(header, "low");
            int closeIdx = findColumnIndex(header, "close");
            int volumeIdx = findColumnIndex(header, "volume");
            
            if (dateTimeIdx < 0 || openIdx < 0 || highIdx < 0 || lowIdx < 0 || closeIdx < 0) {
                throw new IOException("Required columns not found in CSV file: " + file.getPath());
            }
            
            // Create the time series
            TimeSeries timeSeries = new TimeSeries(symbol);
            
            // Skip the header row
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                
                if (row.length <= Math.max(dateTimeIdx, Math.max(openIdx, Math.max(highIdx, Math.max(lowIdx, closeIdx))))) {
                    continue; // Skip incomplete rows
                }
                
                try {
                    LocalDateTime timestamp = LocalDateTime.parse(row[dateTimeIdx], dateTimeFormatter);
                    double open = Double.parseDouble(row[openIdx]);
                    double high = Double.parseDouble(row[highIdx]);
                    double low = Double.parseDouble(row[lowIdx]);
                    double close = Double.parseDouble(row[closeIdx]);
                    long volume = volumeIdx >= 0 && volumeIdx < row.length ? 
                                  Long.parseLong(row[volumeIdx]) : 0;
                    
                    Bar bar = new Bar(timestamp, open, high, low, close, volume);
                    timeSeries.addBar(bar);
                } catch (Exception e) {
                    // Skip rows with parsing errors
                    System.err.println("Error parsing row " + i + ": " + e.getMessage());
                }
            }
            
            return timeSeries;
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Finds the index of a column in the header row.
     * 
     * @param header the header row
     * @param possibleNames possible names of the column (case-insensitive)
     * @return the index of the column, or -1 if not found
     */
    private int findColumnIndex(String[] header, String... possibleNames) {
        for (String name : possibleNames) {
            for (int i = 0; i < header.length; i++) {
                if (header[i].trim().equalsIgnoreCase(name)) {
                    return i;
                }
            }
        }
        return -1;
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
}
