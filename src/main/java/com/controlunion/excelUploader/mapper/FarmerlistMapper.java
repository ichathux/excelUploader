package com.controlunion.excelUploader.mapper;

import com.controlunion.excelUploader.model.FarmerList;
import com.controlunion.excelUploader.model.FarmerListFinal;
import com.controlunion.excelUploader.model.FarmerList_deleted;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface FarmerlistMapper {

    FarmerlistMapper INSTANCE = Mappers.getMapper(FarmerlistMapper.class);
    @Mapping(source = "listid", target = "listid")
    @Mapping(source = "proID", target = "proID")
    @Mapping(source = "auditID", target = "auditID")
    @Mapping(source = "cufarmerID", target = "cufarmerID")
    @Mapping(source = "unitNoEUJAS", target = "unitNoEUJAS")
    @Mapping(source = "farCodeEUJAS", target = "farCodeEUJAS")
    @Mapping(source = "unitNoNOP", target = "unitNoNOP")
    @Mapping(source = "farCodeNOP", target = "farCodeNOP")
    @Mapping(source = "farmerName", target = "farmerName")
    @Mapping(source = "totalArea", target = "totalArea")
    @Mapping(source = "city", target = "city")
    @Mapping(source = "gps", target = "gps")
    @Mapping(source = "address", target = "address")
    @Mapping(source = "dateCert", target = "dateCert")
    @Mapping(source = "aplyRetrospe", target = "aplyRetrospe")
    @Mapping(source = "certification", target = "certification")
    @Mapping(source = "fertilizer", target = "fertilizer")
    @Mapping(source = "ferUseDate", target = "ferUseDate")
    @Mapping(source = "dateConfersion", target = "dateConfersion")
    @Mapping(source = "dateorganic", target = "dateorganic")
    @Mapping(source = "note", target = "note")
    @Mapping(source = "user", target = "user")
    @Mapping(source = "sysTimeStamp", target = "sysTimeStamp")
    @Mapping(source = "eujas_field", target = "eujas_field")
    @Mapping(source = "eujas_harvest", target = "eujas_harvest")
    @Mapping(source = "usda_field", target = "usda_field")
    @Mapping(source = "usda_harvest", target = "usda_harvest")
    @Mapping(source = "farmName", target = "farmName")
    @Mapping(source = "plotCode", target = "plotCode")
    FarmerListFinal farmerListToFarmerListFinal(FarmerList farmerList);

    @Mapping(source = "listid", target = "listid")
    @Mapping(source = "proID", target = "proID")
    @Mapping(source = "auditID", target = "auditID")
    @Mapping(source = "cufarmerID", target = "cufarmerID")
    @Mapping(source = "unitNoEUJAS", target = "unitNoEUJAS")
    @Mapping(source = "farCodeEUJAS", target = "farCodeEUJAS")
    @Mapping(source = "unitNoNOP", target = "unitNoNOP")
    @Mapping(source = "farCodeNOP", target = "farCodeNOP")
    @Mapping(source = "farmerName", target = "farmerName")
    @Mapping(source = "totalArea", target = "totalArea")
    @Mapping(source = "city", target = "city")
    @Mapping(source = "gps", target = "gps")
    @Mapping(source = "address", target = "address")
    @Mapping(source = "dateCert", target = "dateCert")
    @Mapping(source = "aplyRetrospe", target = "aplyRetrospe")
    @Mapping(source = "certification", target = "certification")
    @Mapping(source = "fertilizer", target = "fertilizer")
    @Mapping(source = "ferUseDate", target = "ferUseDate")
    @Mapping(source = "dateConfersion", target = "dateConfersion")
    @Mapping(source = "dateorganic", target = "dateorganic")
    @Mapping(source = "note", target = "note")
    @Mapping(source = "user", target = "user")
    @Mapping(source = "sysTimeStamp", target = "sysTimeStamp")
    @Mapping(source = "eujas_field", target = "eujas_field")
    @Mapping(source = "eujas_harvest", target = "eujas_harvest")
    @Mapping(source = "usda_field", target = "usda_field")
    @Mapping(source = "usda_harvest", target = "usda_harvest")
    @Mapping(source = "farmName", target = "farmName")
    @Mapping(source = "plotCode", target = "plotCode")
    FarmerList_deleted farmerListToFarmerListDelete(FarmerList farmerList);
}