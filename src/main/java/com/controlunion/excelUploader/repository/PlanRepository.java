package com.controlunion.excelUploader.repository;

import com.controlunion.excelUploader.model.Plan;
import com.controlunion.excelUploader.model.Projects;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.ArrayList;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    Optional<ArrayList<Plan>> findAllByProID(Projects proID);
    Optional<Plan> findTopOneByProIDAndCertifiedOrderByPlanIDDesc(Projects proID, boolean isCertified);
}
