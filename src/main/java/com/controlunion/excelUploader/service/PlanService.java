package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.dto.PlanDto;
import com.controlunion.excelUploader.mapper.PlanMapper;
import com.controlunion.excelUploader.model.FarmerList;
import com.controlunion.excelUploader.model.FarmerListFinal;
import com.controlunion.excelUploader.model.Plan;
import com.controlunion.excelUploader.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final ProjectService projectService;
    private final FarmerListFinalService farmerListFinalService;
    private final FarmerListService farmerListService;

    public ResponseEntity<List<PlanDto>> getAllPlansForProId(long proID) {

        Optional<List<Plan>> plans = planRepository.findAllByProID(projectService.getProjectByProjectId(proID));
        if (plans.isPresent()) {
            List<PlanDto> planDtos = new ArrayList<>();
            for (Plan plan : plans.get()) {
                planDtos.add(PlanMapper.INSTANCE.planToPlanDto(plan));
            }
            return ResponseEntity.ok(planDtos);
        }
        return ResponseEntity.noContent().build();
    }

    public Plan getLastCertifiedPlanForProId(int auditId, long proID) {
        log.info("Getting last certified Plan for proId: " + proID);

        Optional<Plan> plan = planRepository.findTopOneByProIDAndCertifiedOrderByPlanIDDesc(projectService.getProjectByProjectId(proID), true);
//        Optional<Plan> plan = planRepository.findTopByPlanIDLessThanAndProIDAndCertified(auditId, (int) proID, true);

        log.info("Last certified Plan: " + plan.get().getAuditNo() + " for proId: " + proID+" last audit id : "+plan.get().getPlanID());
        return plan.orElse(null);
    }

    public Plan getPlanById(int auditId){
        try{
            return planRepository.findById(Long.valueOf(auditId)).orElse(null);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }


    @Transactional
    public ResponseEntity<Plan> certifyFarmerList(int auditId) {
        Plan plan = getPlanById(auditId);
        if (plan != null) {
            plan.setCertified(true);
            plan.setCertifyBy("isuru");
            plan.setCertifier(10);
        }
        Plan plan1 = certifyPlan(plan);
        if (plan1 != null) {

            return ResponseEntity.ok(plan1);
        } else
            return ResponseEntity.internalServerError().build();
    }

    public Plan certifyPlan(Plan plan) {
        try{
            List<FarmerList> farmerLists = farmerListService.getFarmListForProIdAndAuditId2(plan.getProID().getId().intValue(), plan.getPlanID().intValue());
//            List<FarmerListFinal> farmerListFinals = farmerListFinalService.getAllFarmerListByProjectIdAndAuditId(plan.getProID().getId().intValue(), plan.getPlanID().intValue());
//            farmerListFinalService.deleteFarmerListFinals();
            farmerListFinalService.saveToFarmerListOnFarmerListFinal(farmerLists);
            farmerListService.deleteFromFarmerList(farmerLists);
            return planRepository.save(plan);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
