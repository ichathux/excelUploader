package com.controlunion.excelUploader.dto;

import com.controlunion.excelUploader.model.Crop;
import com.controlunion.excelUploader.model.FarmerList;
import com.controlunion.excelUploader.model.FarmerListCrop;
import com.controlunion.excelUploader.model.FarmerListFinal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmerListComparisonDto {
    private HashMap<String, FarmerList> map1;
    private HashMap<String, FarmerListFinal> map2;
    private HashMap<String, Integer> cuidVsFarmarCode;

}
