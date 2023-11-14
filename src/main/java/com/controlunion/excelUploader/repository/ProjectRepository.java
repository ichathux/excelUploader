package com.controlunion.excelUploader.repository;

import com.controlunion.excelUploader.model.Projects;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Projects, Long> {
    Optional<ArrayList<Projects>> findByProNameContaining(String name);
}
