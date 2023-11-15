package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.dto.ComparisonResponseDto;
import com.controlunion.excelUploader.model.*;
import com.controlunion.excelUploader.repository.FarmerlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FarmerListService {

    private final FarmerlistRepository repository;
    private final FarmerListFinalService farmerListFinalService;
    private final CropService cropService;
    private final FarmerListDeletedService farmerListDeletedService;
    private final JDBCBatchInsertService jdbcBatchInsertService;

    @Transactional
    public ResponseEntity<String> saveFarmerList(int proId, int auditId, Iterable<FarmerList> farmerLists) {

        log.info("Start saving on db ");
        ArrayList<FarmerList> farmerListsExist = checkFarmerListAlreadyExistForproidAndAuditID(proId, auditId);

        if (farmerListsExist.isEmpty()) {
            repository.saveAll(farmerLists);
//            jdbcBatchInsertService.insertAsBatchGroup(farmerLists);
        } else {
            log.info("deleted old farmer lists");
            repository.deleteAllByProIDAndAuditID(proId, auditId);
            repository.flush();
            log.info("save new farmer lists");
            repository.saveAll(farmerLists);
//            jdbcBatchInsertService.insertAsBatchGroup(farmerLists);
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

    public ComparisonResponseDto getFarmListForProIdAndAuditId(int proId, int auditId) {
        ComparisonResponseDto comparisonResponseDto = new ComparisonResponseDto();
        try {
            Optional<ArrayList<FarmerList>> farmerList = repository.findAllByProIDAndAuditID(proId, auditId);
            if (farmerList.isPresent()) {
                System.out.println("size of list : "+farmerList.get().size());

                ArrayList<FarmerList> farmerListsNew = farmerList.get().stream()
                        .filter(f -> f.getIsNew() == 1)
                        .peek(f -> f.setChngCropdata(f.getFarmerListCropList().stream()
                                .map(c -> cropService.getCropNameById(c.getCropID()).getCropName())
                                .collect(Collectors.joining(", ")))
                        )
                        .collect(Collectors.toCollection(ArrayList::new));

                ArrayList<FarmerList> farmerListsOldChanged = farmerList.get().stream()
                        .filter(f -> f.getIsChange() == 1)
                        .collect(Collectors.toCollection(ArrayList::new));

                 ArrayList<FarmerList> farmerListsDeleted = farmerList.get().stream()
                        .filter(f -> f.getIsChange() == 3)
                        .collect(Collectors.toCollection(ArrayList::new));


                System.out.println("size of new list : "+farmerListsNew.size());
                System.out.println("size of changes list : "+farmerListsOldChanged.size());
//                ArrayList<FarmerList> farmerLists = farmerList.get().stream()
//                        .map(f -> {
//                            if (f.getIsNew()== 1){
//                                f.setChngCropdata(f.getFarmerListCropList().stream()
//                                        .map(c -> cropService.getCropNameById(c.getCropID()).getCropName())
//                                        .collect(Collectors.joining(", ")));
//
//                                return f;
//                            }else
//                                return f;
//                        })
//                        .collect(Collectors.toCollection(ArrayList::new));
                comparisonResponseDto.setNewFarmerList(farmerListsNew);
                comparisonResponseDto.setExistingFarmerList(farmerListsOldChanged);
                comparisonResponseDto.setDeletedFarmerList(farmerListsDeleted);
            }
            return comparisonResponseDto;
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
