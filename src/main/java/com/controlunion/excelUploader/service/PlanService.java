package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.dto.PlanDto;
import com.controlunion.excelUploader.mapper.FarmerlistFinalMapper;
import com.controlunion.excelUploader.mapper.FarmerlistMapper;
import com.controlunion.excelUploader.mapper.PlanMapper;
import com.controlunion.excelUploader.model.FarmerList;
import com.controlunion.excelUploader.model.FarmerListFinal;
import com.controlunion.excelUploader.model.FarmerList_deleted;
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
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final ProjectService projectService;
    private final FarmerListFinalService farmerListFinalService;
    private final FarmerListService farmerListService;
    private final FarmerListFinalCropsService farmerListFinalCropsService;
    private final FarmerListDeletedService farmerListDeletedService;

    public ResponseEntity<List<PlanDto>> getAllPlansForProId(long proID) {
        try{
            Optional<ArrayList<Plan>> plans = planRepository.findAllByProID(projectService.getProjectByProjectId(proID));
            if (plans.isPresent()) {
                List<PlanDto> planDtos = new ArrayList<>();
                for (Plan plan : plans.get()) {
                    planDtos.add(PlanMapper.INSTANCE.planToPlanDto(plan));
                }
                return ResponseEntity.ok(planDtos);
            }
            return ResponseEntity.noContent().build();
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }

    }

    public Plan getLastCertifiedPlanForProId(int auditId, long proID) {
        try{
            log.info("Getting last certified Plan for proId: " + proID);
            Optional<Plan> plan = planRepository.findTopOneByProIDAndCertifiedOrderByPlanIDDesc(

                    projectService.getProjectByProjectId(proID),
                    true);
//            log.info("found last certified Plan: " + plan.get().getPlanID());

            return plan.orElse(null);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }

    }

    public Plan getPlanById(int auditId) {
        try {
            return planRepository.findById(Long.valueOf(auditId)).orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    @Transactional
    public ResponseEntity<Plan> certifyFarmerList(int auditId) {
        try {
            Plan plan = getPlanById(auditId);
            if (plan != null) {
                plan.setCertified(true);
                plan.setCertifyBy("isuru");
                plan.setCertifier(10);
            }
            Plan plan1 = certifyPlan(plan);
            if (plan1 != null) {
                System.out.println("Done certifying");
                return ResponseEntity.ok(plan1);
            }else
                return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }

    }

    public Plan certifyPlan(Plan plan) {
        try {
            ArrayList<FarmerList> farmerLists = farmerListService.getFarmListForProIdAndAuditId2(
                    plan.getProID().getId().intValue(),
                    plan.getPlanID().intValue());

            ArrayList<FarmerListFinal> farmerListsFarmerListFinals = farmerListFinalService.getAllFarmerListByProjectIdAndAuditId(
                    plan.getProID().getId().intValue(),
                    plan.getPlanID().intValue());

//            ArrayList<FarmerList_deleted> farmerListsDeleted =
//                    .collect(Collectors.toCollection(ArrayList::new));

            farmerListDeletedService.addDataToFarmListDeleted(farmerLists, plan.getPlanID().intValue());
            farmerLists = farmerLists.stream()
                    .filter(f -> f.getIsChange() != 3)
                    .collect(Collectors.toCollection(ArrayList::new));

            if (farmerListsFarmerListFinals.isEmpty()){
                System.out.println("no prev finals");
//                farmerListService.deleteFromFarmerList(farmerLists);
                farmerListFinalService.saveToFarmerListFinal(farmerLists);
                return planRepository.save(plan);
            }else{
                System.out.println("have prev finals"+farmerListsFarmerListFinals.size());
                System.out.println("deleting prev crops final");
                farmerListFinalCropsService.deleteFarmerListCropFinalByFarmerListFinal(farmerListsFarmerListFinals);
                System.out.println("deleting prev farmerlist final");
                farmerListFinalService.deleteFarmerListFinals(farmerListsFarmerListFinals);
                System.out.println("save farmerlist final new");
                farmerListFinalService.saveToFarmerListFinal(farmerLists);
                System.out.println("save plan");
                return planRepository.save(plan);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
