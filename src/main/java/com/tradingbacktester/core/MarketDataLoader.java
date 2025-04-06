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


public class MarketDataLoader {
    private String dataDirectory;
    private DateTimeFormatter dateTimeFormatter;

    public MarketDataLoader() {
        this("./data", "yyyy-MM-dd HH:mm:ss");
    }
    
    public MarketDataLoader(String dataDirectory, String dateTimeFormat) {
        this.dataDirectory = dataDirectory;
        this.dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormat);
    }
    
    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }
    
    public void setDateTimeFormat(String dateTimeFormat) {
        this.dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormat);
    }
    
    public TimeSeries loadData(String symbol, LocalDateTime startTime, LocalDateTime endTime) {
        // First,  load from a CSV file
        TimeSeries data = loadFromCsv(symbol);
        
        if (data == null) {
            throw new RuntimeException("Could not load data for symbol: " + symbol);
        }
        
        return filterTimeRange(data, startTime, endTime);
    }
    
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
            
            // Determine the column indices (assumes the first row is the header)
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
                    System.err.println("Error parsing row " + i + ": " + e.getMessage());
                }
            }
            
            return timeSeries;
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
            return null;
        }
    }
    
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
