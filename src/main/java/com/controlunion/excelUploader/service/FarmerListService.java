package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.model.FarmerList;
import com.controlunion.excelUploader.repository.FarmerlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FarmerListService {

    private final FarmerlistRepository farmerlistRepository;

    @Transactional
    public ResponseEntity<String> saveFarmerList(Iterable<FarmerList> farmerLists) {

        try {
//            for (FarmerList farmerList : farmerLists) {
//                try {
//                    log.info("saving : " + farmerList);
//
//                    log.info("saved : ");
//                } catch (RuntimeException e) {
//                    log.error(e.getMessage());
//                } catch (Exception e) {
//                    throw e;
//                }
//
//            }
//            farmerlistRepository.saveAll(farmerLists);
            log.info("saving user data to DB - success ");
            return ResponseEntity.ok().body("Done");
        } catch (Exception e) {
            log.error("error occurred while saving user data to DB : " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(e.getMessage());
        }

    }

    public int getLastFarmListId() {
        int listId = 1;
        FarmerList farmerList = farmerlistRepository.findFirstByOrderByListidDesc().orElse(null);

        if (farmerList != null) {
            listId = farmerList.getListid();
        }
        System.out.println("last list id : " + listId);
        return listId;
    }
}
