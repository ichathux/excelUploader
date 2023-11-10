package com.controlunion.excelUploader.dto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CropChangesDto {
    private int cropId;
    private String changedField;
    private int last;
    private int current;

}
