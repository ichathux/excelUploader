package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.model.*;
import com.controlunion.excelUploader.repository.FarmerlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FarmerListService {

    private final FarmerlistRepository repository;
    private final FarmerListFinalService farmerListFinalService;

    @Transactional
    public ResponseEntity<String> saveFarmerList(int proId, int auditId, Iterable<FarmerList> farmerLists) {

        log.info("Start saving on db ");
        ArrayList<FarmerList> farmerListsExist = checkFarmerListAlreadyExistForproidAndAuditID(proId, auditId);

        if (farmerListsExist.isEmpty()) {
            repository.saveAll(farmerLists);
        } else {
            log.info("deleted old farmer lists");
            repository.deleteAllByProIDAndAuditID(proId, auditId);
            repository.flush();
            log.info("save new farmer lists");
            repository.saveAll(farmerLists);
        }
        return ResponseEntity.badRequest().build();
    }

    private ArrayList<FarmerList> checkFarmerListAlreadyExistForproidAndAuditID(int proId, int auditId) {
        try {
            return repository.findAllByProIDAndAuditID(proId, auditId).orElse(new ArrayList<>());
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public ArrayList getFarmListForProIdAndAuditId(int proId, int auditId) {
        try {
            Optional<ArrayList<FarmerList>> farmerList = repository.findAllByProIDAndAuditID(proId, auditId);
            if (farmerList.isPresent()) {
                System.out.println("size of list : "+farmerList.get().size());
                return farmerList.get();
            } else {
                ArrayList<FarmerListFinal> farmerListFinals = farmerListFinalService.getAllFarmerListByProjectIdAndAuditId(proId, auditId);
                if (farmerListFinals != null) {
                    return farmerListFinals;
                } else {
                    return new ArrayList();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public ArrayList<FarmerList> getFarmListForProIdAndAuditId2(int proId, int auditId) {
        try {
            System.out.println("getting farmerlist for " + proId + " " + auditId);
            Optional<ArrayList<FarmerList>> farmerList = repository.findAllByProIDAndAuditID(proId, auditId);
            return farmerList.orElseGet(ArrayList::new);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

}
