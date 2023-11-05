package com.controlunion.excelUploader.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@Builder
public class ExcelErrorResponse {

    private String location;
    private String error;
    private String errorValue;
    private String correctValue;

}
