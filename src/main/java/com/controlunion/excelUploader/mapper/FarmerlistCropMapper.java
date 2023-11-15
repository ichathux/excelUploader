package com.controlunion.excelUploader.mapper;

import com.controlunion.excelUploader.model.FarmerListCrop;
import com.controlunion.excelUploader.model.FarmerListCropFinal;
import com.controlunion.excelUploader.model.FarmerListCrop_deleted;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface FarmerlistCropMapper {

    FarmerlistCropMapper INSTANCE = Mappers.getMapper(FarmerlistCropMapper.class);

//    @Mapping(source = "id", target = "id")
    @Mapping(source = "cropID", target = "cropID")
    @Mapping(source = "cufarmerID", target = "cufarmerID")
    @Mapping(source = "plotCode", target = "plotCode")
    @Mapping(source = "noOfPlant", target = "noOfPlant")
    @Mapping(source = "estiYield", target = "estiYield")
    @Mapping(source = "realYield", target = "realYield")
    @Mapping(source = "noOfSesons", target = "noOfSesons")
    FarmerListCrop_deleted farmerListCropFinalToFarmerListCropDeleted(FarmerListCropFinal farmerListCropFinal);

    @Mapping(source = "cropID", target = "cropID")
    @Mapping(source = "cufarmerID", target = "cufarmerID")
    @Mapping(source = "plotCode", target = "plotCode")
    @Mapping(source = "noOfPlant", target = "noOfPlant")
    @Mapping(source = "estiYield", target = "estiYield")
    @Mapping(source = "realYield", target = "realYield")
    @Mapping(source = "noOfSesons", target = "noOfSesons")
    FarmerListCrop_deleted farmerListCropToFarmerListCropDeleted(FarmerListCrop farmerListCrop);

    @Mapping(source = "cropID", target = "cropID")
    @Mapping(source = "cufarmerID", target = "cufarmerID")
    @Mapping(source = "plotCode", target = "plotCode")
    @Mapping(source = "noOfPlant", target = "noOfPlant")
    @Mapping(source = "estiYield", target = "estiYield")
    @Mapping(source = "realYield", target = "realYield")
    @Mapping(source = "noOfSesons", target = "noOfSesons")
    FarmerListCropFinal farmerListCropToFarmerLIstCropFinal(FarmerListCrop farmerListCrop);


}
