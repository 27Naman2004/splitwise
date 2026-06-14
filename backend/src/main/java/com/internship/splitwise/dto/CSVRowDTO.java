package com.internship.splitwise.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CSVRowDTO {
    private Integer rowNumber;
    private String date;
    private String description;
    private String paidBy;
    private String amount;
    private String currency;
    private String splitType;
    private String splitWith;
    private String splitDetails;
    private String notes;
    private String originalLine;
}
