package com.controlunion.excelUploader.controller;

import com.controlunion.excelUploader.dto.PlanDto;
import com.controlunion.excelUploader.model.Plan;
import com.controlunion.excelUploader.repository.PlanRepository;
import com.controlunion.excelUploader.service.PlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/audit/v1")
@Slf4j
public class AuditController {

    private final PlanService planService;

    @GetMapping("getAuditPlansByProId")
    public ResponseEntity<List<PlanDto>> getAllAuditPlansByProId(@RequestParam("proId") long proId){
        log.info("Get audit plans for proID "+proId);
        return planService.getAllPlansForProId(proId);
    }
}
