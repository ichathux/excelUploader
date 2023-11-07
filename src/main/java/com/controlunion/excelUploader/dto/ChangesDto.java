package com.controlunion.excelUploader.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

@ToString
@NoArgsConstructor
@Data
public class ChangesDto {
    private String last;
    private String current;

    public ChangesDto(String current) {
        this.current = current;
    }

    public ChangesDto(Number current) {
        this.current = String.valueOf(current);
    }

    public ChangesDto(Number last, Number current) {
        this.last = String.valueOf(last);
        this.current = String.valueOf(current);
    }

    public ChangesDto(String last, String current) {
        this.last = last;
        this.current = current;
    }
}
