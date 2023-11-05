package com.controlunion.excelUploader.dto;

import lombok.Data;

@Data
public class FarmerListMandatoryFieldsDto {

    private boolean isUnitNoEUJAS = false;
    private boolean isFarCodeEUJAS = false;
    private boolean isUnitNoNOP = false;
    private boolean isFarCodeNOP = false;
    private boolean isFarmerName = false;
    private boolean isFarmName = false;
    private boolean isPlotCode = false;
    private boolean isTotalArea = false;
    private boolean isCity = false;
    private boolean isStartingDateCon = false;
    private boolean isStartingDateOrg = false;
    private boolean isEujasField = false;
    private boolean isEujasHarvest = false;

    void checkManExists(FarmerListMandatoryFieldsDto dto){
    }
}
