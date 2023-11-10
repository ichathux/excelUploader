package com.controlunion.excelUploader.controller;

import com.controlunion.excelUploader.model.FarmerList_deleted;
import com.controlunion.excelUploader.service.FarmerListDeletedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/deleted_farmers/v1")
@RequiredArgsConstructor
public class DeletedFarmerController {

    private final FarmerListDeletedService farmerListDeletedService;

    @GetMapping("getDeletedList")
    public ResponseEntity<List<FarmerList_deleted>> getDeletedFarmersByProjectAndAudit(@RequestParam("proId") int proID,
                                                                                       @RequestParam("auditId") int auditId){
        return ResponseEntity.ok(farmerListDeletedService.getAllByProIdAndAuditId(proID, auditId));
    }
}
