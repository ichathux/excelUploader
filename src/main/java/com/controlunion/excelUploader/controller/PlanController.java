package com.controlunion.excelUploader.controller;

import com.controlunion.excelUploader.model.Plan;
import com.controlunion.excelUploader.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/plan/v1")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @PostMapping("certify")
    public ResponseEntity<Plan> certifyFarmerList(@RequestParam("auditID") String auditId){
        System.out.println("certifying");
        System.out.println(auditId);
        return planService.certifyFarmerList(Integer.parseInt(auditId));
    }
}
