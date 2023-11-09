package com.controlunion.excelUploader.dto;

import com.controlunion.excelUploader.model.Crop;
import com.controlunion.excelUploader.model.FarmerListCrop;
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
    private String change;
    private int cropId;
    private FarmerListCrop crop;

    public ChangesDto(Number current) {
        this.current = String.valueOf(current);
    }

    public ChangesDto(Number last, Number current) {
        this.last = String.valueOf(last);
        this.current = String.valueOf(current);
    }
    public ChangesDto(int cropId, String chnage, Number last, Number current) {
        this.last = String.valueOf(last);
        this.current = String.valueOf(current);
        this.change = chnage;
        this.cropId = cropId;
    }

    public ChangesDto(int cropId, String chnage, Number last, Number current, FarmerListCrop farmerListCrop) {
        this.last = String.valueOf(last);
        this.current = String.valueOf(current);
        this.change = chnage;
        this.cropId = cropId;
        this.crop = farmerListCrop;
    }

    public ChangesDto(String last, String current) {
        this.last = last;
        this.current = current;
    }
    public ChangesDto(String chng, String last, String current) {
        this.last = last;
        this.current = current;
        this.change = chng;
    }
}
