package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.dto.ComparisonResponseDto;
import com.controlunion.excelUploader.model.*;
import com.controlunion.excelUploader.repository.FarmerlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FarmerListService {

    private final FarmerlistRepository farmerListRepository;
    private final CropService cropService;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void saveFarmerList(int proId, int auditId, Iterable<FarmerList> farmerLists) {
        farmerListRepository.flush();
        try {
            log.info("Start saving on db " + proId + " " + auditId);
            ArrayList<FarmerList> farmerListsExist = checkFarmerListAlreadyExistForproidAndAuditID(proId, auditId);
            System.out.println("size " + farmerListsExist.size());
//            System.out.println(farmerLists);
            if (farmerListsExist.isEmpty()) {
                System.out.println("not contain");
                farmerListRepository.findAllByProIDAndAuditID(proId, auditId);
//                for (FarmerList farmerList : farmerLists) {
//                    System.out.println("sacving " + farmerList);
//                    farmerListRepository.saveAndFlush(farmerList);
//                }
//                farmerLists = farmerListRepository.saveAll(farmerLists);
//                farmerLists = farmerListRepository.saveAllAndFlush(farmerLists);
//                System.out.println(farmerLists);
//                return ResponseEntity.ok().build();
                farmerListRepository.saveAllAndFlush(farmerLists);
            } else {
                System.out.println("contain");
                log.info("deleted old farmer lists " + proId + " " + auditId);
                farmerListRepository.flush();
                farmerListRepository.deleteAllByProIDAndAuditID(proId, auditId);
                farmerListRepository.flush();
                log.info("deleted");
                farmerListRepository.findAllByProIDAndAuditID(proId, auditId);
                log.info("save new farmer lists");
                farmerListRepository.saveAllAndFlush(farmerLists);
                log.info("save");
//                return ResponseEntity.ok().build();
            }
        } catch (Exception e) {
            System.out.println("error occured" + e.getMessage());
            e.printStackTrace();
//            return ResponseEntity.badRequest().build();
        }

    }

    private ArrayList<FarmerList> checkFarmerListAlreadyExistForproidAndAuditID(int proId, int auditId) {
        try {
            return farmerListRepository.findAllByProIDAndAuditID(proId, auditId).orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public ComparisonResponseDto getFarmListForProIdAndAuditId(int proId, int auditId) {
        ComparisonResponseDto comparisonResponseDto = new ComparisonResponseDto();
        try {
            Optional<ArrayList<FarmerList>> farmerList = farmerListRepository.findAllByProIDAndAuditID(proId, auditId);
            if (farmerList.isPresent()) {
                System.out.println("size of list : " + farmerList.get().size());

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


                System.out.println("size of new list : " + farmerListsNew.size());
                System.out.println("size of changes list : " + farmerListsOldChanged.size());
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
            Optional<ArrayList<FarmerList>> farmerList = farmerListRepository.findAllByProIDAndAuditID(proId, auditId);
            return farmerList.orElseGet(ArrayList::new);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

}
