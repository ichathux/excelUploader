package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.model.FarmerList;
import com.controlunion.excelUploader.model.FarmerListCrop;
import com.controlunion.excelUploader.repository.FarmerListCropRepository;
import com.controlunion.excelUploader.repository.FarmerlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FarmerListService {

    private final FarmerlistRepository farmerlistRepository;
    private final FarmerListCropRepository farmerListCropRepository;

    @Transactional
    public ResponseEntity<String> saveFarmerList(int proId, int auditId, Iterable<FarmerList> farmerLists) throws InterruptedException {
        log.info("Start saving on db ");
        try {
            ArrayList<FarmerList> farmerListsExist = checkFarmerListAlreadyExistForproidAndAuditID(proId, auditId);
            for (FarmerList farmerList : farmerLists){

                if (!farmerListsExist.isEmpty()){
//                    System.out.println("already contained data");
                    FarmerList fl = farmerListsExist.stream()
                            .filter(f -> f.getCufarmerID() == farmerList.getCufarmerID()
                            && f.getPlotCode().contentEquals(farmerList.getPlotCode()))
                            .findFirst().orElse(null);
                    if (fl != null){
                        removeExistingData(farmerList, fl);
                    }
//                    System.out.println(fl);
//                    System.out.println(farmerList);
                }
//                System.out.println("start saving data");
                farmerlistRepository.save(farmerList);
            }
            log.info("saving user data to DB - success ");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("error occurred while saving user data to DB : " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

    }

    private void removeExistingData(FarmerList farmerList, FarmerList fl) {
//        System.out.println("already contained data-1");
        farmerList.setListid(fl.getListid());
        farmerlistRepository.delete(fl);
        farmerlistRepository.findFarmerListByProIDAndAuditID(farmerList.getProID(), farmerList.getAuditID()).isPresent();
//        System.out.println("deleted-1");
    }

    private ArrayList<FarmerList> checkFarmerListAlreadyExistForproidAndAuditID(int proId, int auditId) {
        return farmerlistRepository.findAllByProIDAndAuditID(proId, auditId).orElse(new ArrayList<>());
    }


    public ResponseEntity<List<FarmerList>> getFarmListForProIdAndAuditId(int proId, int auditId) {
        Optional<List<FarmerList>> farmerList = farmerlistRepository.findFarmerListByProIDAndAuditID(proId, auditId);
        return farmerList.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }


}
