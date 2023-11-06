package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.dto.PlanDto;
import com.controlunion.excelUploader.mapper.PlanMapper;
import com.controlunion.excelUploader.model.Plan;
import com.controlunion.excelUploader.model.Projects;
import com.controlunion.excelUploader.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final ProjectService projectService;

    public ResponseEntity<List<PlanDto>> getAllPlansForProId(long proID){

        Optional<List<Plan>> plans = planRepository.findAllByProID(projectService.getProjectByProjectId(proID));
        if (plans.isPresent()){
            List<PlanDto> planDtos = new ArrayList<>();
            for (Plan plan : plans.get()){
                planDtos.add(PlanMapper.INSTANCE.planToPlanDto(plan));
            }
            return ResponseEntity.ok(planDtos);
        }
        return ResponseEntity.noContent().build();
    }
}
