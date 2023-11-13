package com.controlunion.excelUploader.controller;

import com.controlunion.excelUploader.model.FarmerList;
import com.controlunion.excelUploader.model.Plan;
import com.controlunion.excelUploader.service.FarmerListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("api/farmerlist/v1")
public class FarmerListController {

    private final FarmerListService farmerListService;

    @GetMapping("getFarmList")
    public ResponseEntity<List> getFarmerListChanges(@RequestParam("proId") int proId,
                                                                @RequestParam("auditId") int auditId){
        log.info("got from back end : project Id "+proId +" auditId : "+auditId);
        return ResponseEntity.ok().body(farmerListService.getFarmListForProIdAndAuditId(proId, auditId));
    }



}

