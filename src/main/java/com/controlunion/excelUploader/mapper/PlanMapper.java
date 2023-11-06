package com.controlunion.excelUploader.mapper;

import com.controlunion.excelUploader.dto.PlanDto;
import com.controlunion.excelUploader.model.Plan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PlanMapper {

    PlanMapper INSTANCE = Mappers.getMapper(PlanMapper.class);

    @Mappings({
            @Mapping(source = "planID", target = "planId"),
            @Mapping(source = "auditNo", target = "auditNo")
    })
    PlanDto planToPlanDto(Plan plan);
}
