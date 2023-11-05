package com.controlunion.excelUploader.model.comp_keys;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class FarmerListCropID implements Serializable {
    private int cufarmerID;
    private String plotCode;
    private int cropID;
}

