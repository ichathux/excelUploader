package com.controlunion.excelUploader.controller;


import com.controlunion.excelUploader.dto.ProjectDto;
import com.controlunion.excelUploader.model.Projects;
import com.controlunion.excelUploader.repository.ProjectRepository;
import com.controlunion.excelUploader.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/project/v1/")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<List<Projects>> getAllProjects(){
        return projectService.getAllProjects();
    }

    @GetMapping("/search")
    public ResponseEntity<ArrayList<ProjectDto>> searchProject(@RequestParam("name") String name){
        return projectService.getProjectsByName(name);
    }
}
