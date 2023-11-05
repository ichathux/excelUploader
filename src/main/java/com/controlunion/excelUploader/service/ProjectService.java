package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.model.Projects;
import com.controlunion.excelUploader.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ResponseEntity<List<Projects>> getAllProjects(){
        return ResponseEntity.ok().body(projectRepository.findAll());
    }

    public ResponseEntity<List<Projects>> getProjectsByName(String name) {
        List<Projects> projects = projectRepository.findByProNameContaining(name).orElseThrow(() -> new RuntimeException("No projects found for given name"));
        return ResponseEntity.ok(projects);
    }
}
