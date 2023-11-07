package com.controlunion.excelUploader.controller;

import com.controlunion.excelUploader.model.FarmerList;
import com.controlunion.excelUploader.service.FarmerListService;
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
@Slf4j
@RequestMapping("api/farmerlist/v1")
public class FarmerListController {

    private final FarmerListService farmerListService;

    @GetMapping("getFarmList")
    public ResponseEntity<List<FarmerList>> getFarmerListChanges(@RequestParam("proId") int proId,
                                                                @RequestParam("auditId") int auditId){
        log.info("got from back end : project Id "+proId +" auditId : "+auditId);
        return farmerListService.getFarmListForProIdAndAuditId(proId, auditId);
    }

}

