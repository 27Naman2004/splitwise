package com.internship.splitwise.service;

import com.internship.splitwise.dto.CSVRowDTO;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class CSVParser {

    /**
     * Parses a CSV input stream line-by-line using a state machine that handles quoted fields.
     */
    public List<CSVRowDTO> parse(InputStream inputStream) throws Exception {
        List<CSVRowDTO> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            
            // Skip the header row
            String header = reader.readLine();
            if (header == null) {
                throw new IllegalArgumentException("CSV file is empty");
            }
            lineNumber++; // Header is row 1
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue; // Skip empty rows
                }
                
                List<String> fields = parseLine(line);
                
                // Map fields to DTO
                CSVRowDTO row = CSVRowDTO.builder()
                        .rowNumber(lineNumber)
                        .date(getFieldSafe(fields, 0))
                        .description(getFieldSafe(fields, 1))
                        .paidBy(getFieldSafe(fields, 2))
                        .amount(getFieldSafe(fields, 3))
                        .currency(getFieldSafe(fields, 4))
                        .splitType(getFieldSafe(fields, 5))
                        .splitWith(getFieldSafe(fields, 6))
                        .splitDetails(getFieldSafe(fields, 7))
                        .notes(getFieldSafe(fields, 8))
                        .originalLine(line)
                        .build();
                
                rows.add(row);
            }
        }
        return rows;
    }

    private List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes; // Toggle quote state
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString().trim());
                currentField.setLength(0); // Reset builder
            } else {
                currentField.append(c);
            }
        }
        // Add the last field
        fields.add(currentField.toString().trim());
        
        return fields;
    }

    private String getFieldSafe(List<String> fields, int index) {
        if (index >= 0 && index < fields.size()) {
            return fields.get(index);
        }
        return "";
    }
}
