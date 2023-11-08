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
    public ResponseEntity<String> saveFarmerList(Iterable<FarmerList> farmerLists) {
        log.info("Start saving on db ");
        try {
            for (FarmerList farmerList : farmerLists){

//                ArrayList<FarmerListCrop> farmerListCrops = new ArrayList<>();
//                for (FarmerListCrop crop : farmerList.getFarmerListCropList()){
//                    System.out.println("adding crop : "+crop);
//                    crop = farmerListCropRepository.save(crop);
//                    farmerListCrops.add(crop);
//                }
//                System.out.println(farmerListCrops.size());
//                farmerList.setFarmerListCropList(farmerListCrops);
                farmerlistRepository.save(farmerList);
            }
            log.info("saving user data to DB - success ");
            return ResponseEntity.ok().body("Done");
        } catch (Exception e) {
            log.error("error occurred while saving user data to DB : " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(e.getMessage());
        }

    }


    public ResponseEntity<List<FarmerList>> getFarmListForProIdAndAuditId(int proId, int auditId) {
        Optional<List<FarmerList>> farmerList = farmerlistRepository.findFarmerListByProIDAndAuditID(proId, auditId);
        return farmerList.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }
}
