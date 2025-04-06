package com.tradingbacktester.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for reading and writing CSV files.
 */
public final class CsvUtils {
    
    /**
     * Reads a CSV file and returns the data as a list of string arrays.
     * 
     * @param filePath the path to the CSV file
     * @return a list of string arrays, one for each row
     * @throws IOException if an I/O error occurs
     */
    public static List<String[]> readCsv(String filePath) throws IOException {
        List<String[]> data = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath));
             CSVParser parser = CSVFormat.DEFAULT.withHeader().parse(reader)) {
            
            // Add header
            data.add(parser.getHeaderNames().toArray(new String[0]));
            
            // Add data rows
            for (CSVRecord record : parser) {
                String[] row = new String[record.size()];
                for (int i = 0; i < record.size(); i++) {
                    row[i] = record.get(i);
                }
                data.add(row);
            }
        }
        
        return data;
    }
    
    /**
     * Writes data to a CSV file.
     * 
     * @param filePath the path to the CSV file
     * @param data the data to write, as a list of string arrays
     * @throws IOException if an I/O error occurs
     */
    public static void writeCsv(String filePath, List<String[]> data) throws IOException {
        if (data.isEmpty()) {
            return;
        }
        
        try (FileWriter writer = new FileWriter(filePath);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            
            for (String[] row : data) {
                printer.printRecord((Object[]) row);
            }
        }
    }
    
    /**
     * Reads a CSV file with headers and returns the data as a list of maps.
     * 
     * @param filePath the path to the CSV file
     * @return a list of CSV records, each as a map of column name to value
     * @throws IOException if an I/O error occurs
     */
    public static List<CSVRecord> readCsvWithHeaders(String filePath) throws IOException {
        List<CSVRecord> records = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath));
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
            
            records.addAll(parser.getRecords());
        }
        
        return records;
    }
    
    /**
     * Converts a CSV record to a string array.
     * 
     * @param record the CSV record
     * @return the string array
     */
    public static String[] recordToStringArray(CSVRecord record) {
        String[] row = new String[record.size()];
        for (int i = 0; i < record.size(); i++) {
            row[i] = record.get(i);
        }
        return row;
    }
    
    // Prevent instantiation
    private CsvUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
