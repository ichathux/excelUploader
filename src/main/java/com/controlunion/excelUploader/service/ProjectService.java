package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.dto.ProjectDto;
import com.controlunion.excelUploader.mapper.ProjectMapper;
import com.controlunion.excelUploader.model.Projects;
import com.controlunion.excelUploader.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ResponseEntity<List<Projects>> getAllProjects() {
        try{
            return ResponseEntity.ok().body(projectRepository.findAll());
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    public ResponseEntity<List<ProjectDto>> getProjectsByName(String name) {
        try {
            List<Projects> projects = projectRepository
                    .findByProNameContaining(name)
                    .orElseThrow(() -> new RuntimeException("No projects found for given name"));

            return ResponseEntity
                    .ok(projects.stream()
                                    .map(p -> ProjectMapper.INSTANCE.projectsToProjectDto(p))
                                    .collect(Collectors.toList()
                                    ));
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }

    }

    public Projects getProjectByProjectId(long proId) {
        try {
            return projectRepository.findById(proId).orElse(null);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
