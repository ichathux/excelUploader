package com.controlunion.excelUploader.repository;

import com.controlunion.excelUploader.model.Plan;
import com.controlunion.excelUploader.model.Projects;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    Optional<List<Plan>> findAllByProID(Projects proID);
}
