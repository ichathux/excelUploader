package com.controlunion.excelUploader.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@AllArgsConstructor
@ToString
public class ChangesDto {
    private String last;
    private String current;

    public ChangesDto(String current) {
        this.current = current;
    }
}
