package com.controlunion.excelUploader.mapper;

import com.controlunion.excelUploader.dto.ProjectDto;
import com.controlunion.excelUploader.model.Projects;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ProjectMapper {

    ProjectMapper INSTANCE = Mappers.getMapper(ProjectMapper.class);
    @Mapping(source = "id", target = "id")
    @Mapping(source = "proName", target = "proName")
    @Mapping(source = "proCode", target = "proCode")
    ProjectDto projectsToProjectDto(Projects plan);
}
