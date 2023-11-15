package com.controlunion.excelUploader.dto;

import com.controlunion.excelUploader.model.FarmerList;
import com.controlunion.excelUploader.model.FarmerList_deleted;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;

//@AllArgsConstructor
@Data
public class ComparisonResponseDto {

    private ArrayList<FarmerList> newFarmerList;
    private ArrayList<FarmerList> existingFarmerList;
    private ArrayList<FarmerList> deletedFarmerList;
}
