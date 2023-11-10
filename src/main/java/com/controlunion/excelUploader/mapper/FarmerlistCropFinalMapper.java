package com.controlunion.excelUploader.mapper;

import com.controlunion.excelUploader.model.FarmerListCropFinal;
import com.controlunion.excelUploader.model.FarmerListCrop_deleted;
import com.controlunion.excelUploader.model.FarmerListFinal;
import com.controlunion.excelUploader.model.FarmerList_deleted;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface FarmerlistCropFinalMapper {

    FarmerlistCropFinalMapper INSTANCE = Mappers.getMapper(FarmerlistCropFinalMapper.class);

//    @Mapping(source = "id", target = "id")
    @Mapping(source = "cropID", target = "cropID")
    @Mapping(source = "cufarmerID", target = "cufarmerID")
    @Mapping(source = "plotCode", target = "plotCode")
    @Mapping(source = "noOfPlant", target = "noOfPlant")
    @Mapping(source = "estiYield", target = "estiYield")
    @Mapping(source = "realYield", target = "realYield")
    @Mapping(source = "noOfSesons", target = "noOfSesons")
    FarmerListCrop_deleted farmerListCropFinalToFarmerListCropDeleted(FarmerListCropFinal farmerListCropFinal);
}
