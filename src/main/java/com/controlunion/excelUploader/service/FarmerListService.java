package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.model.*;
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

    private final FarmerlistRepository repository;
    private final FarmerListFinalService farmerListFinalService;
    private final FarmerlistCropService farmerlistCropService;

    int maxRetries = 3;

    @Transactional
    public ResponseEntity<String> saveFarmerList(int proId, int auditId, Iterable<FarmerList> farmerLists) {
        log.info("Start saving on db ");
        int retries = 0;
        ArrayList<FarmerList> farmerListsExist = checkFarmerListAlreadyExistForproidAndAuditID(proId, auditId);
        if (farmerListsExist.isEmpty()) {
            repository.saveAll(farmerLists);
        } else {
            System.out.println("deleted old farmer lists");
            repository.deleteAllByProIDAndAuditID(proId, auditId);
            System.out.println("get old list");
            repository.findAllByProIDAndAuditID(proId, auditId);
            System.out.println("sabe new farmerlist");
            repository.saveAll(farmerLists);

//            System.out.println("already contain db");
////            deleteFromFarmerList(farmerListsExist, farmerLists);
//            ArrayList<FarmerList> needToDeleteList = createDeleteFarmerList(farmerListsExist, farmerLists);
//            List<FarmerListCrop> needToDeletedFarmerListFCrops = farmerlistCropService.createDeleteFarmerListCrops(needToDeleteList);
//            System.out.println("*******************************");
//
//            System.out.println(needToDeletedFarmerListFCrops);
////            deleteAllFarmerListCrops()
//            deleteFromFarmerList(needToDeleteList);
////            repository.saveAll(farmerLists);
////            for (FarmerList farmerList : farmerLists){
////                repository.save(farmerList);
////            }
        }
//        while (retries < maxRetries){
//            try {
//                ArrayList<FarmerList> list = new ArrayList<>();
//                ArrayList<FarmerList> farmerListsExist = checkFarmerListAlreadyExistForproidAndAuditID(proId, auditId);
//
//                if (!farmerListsExist.isEmpty()) {
//
//                    for (FarmerList farmerList : farmerLists) {
//
//                        FarmerList fl = farmerListsExist.stream()
//                                .filter(f -> f.getFarCodeEUJAS().equals(farmerList.getFarCodeEUJAS())
//                                        && f.getPlotCode().contentEquals(farmerList.getPlotCode()))
//                                .findFirst().orElse(null);
//
//                        if (fl != null) {
//
//                            farmerList.setListid(fl.getListid());
//                            List<FarmerListCrop> farmerListCropsnew = new ArrayList<>();
//                            for (FarmerListCrop c : fl.getFarmerListCropList()) {
//
//                                FarmerListCrop fc = farmerList.getFarmerListCropList().stream()
//                                        .filter(c1 -> c1.getCropID() == c.getCropID())
//                                        .findFirst()
//                                        .orElse(null);
//
//                                if (fc != null) {
//                                    fc.setId(c.getId());
//                                }
//
//                                farmerListCropsnew.add(fc);
//                            }
//                            farmerList.setFarmerListCropList(farmerListCropsnew);
//                        } else {
//                            System.out.println("no element found for " + farmerList.getCufarmerID() + " " + farmerList.getPlotCode());
//                        }
//                        list.add(farmerList);
//                    }
//                    farmerlistRepository.saveAll(list);
//                } else {
//                    farmerlistRepository.saveAll(farmerLists);
//                }
//                log.info("saving user data to DB - success ");
//                return ResponseEntity.ok().build();
//            } catch (Exception e) {
//                log.error("error occurred while saving user data to DB : " + e.getMessage());
//                e.printStackTrace();
//                retries++;
//            }
//        }
        return ResponseEntity.badRequest().build();
    }

    private ArrayList<FarmerList> createDeleteFarmerList(ArrayList<FarmerList> farmerListsExist, Iterable<FarmerList> farmerLists) {
        ArrayList<FarmerList> deletedList = new ArrayList<>();
        System.out.println(farmerListsExist.size());
        System.out.println();
        try {
            for (FarmerList fl : farmerLists) {
                System.out.println("checking " + fl.getCufarmerID() +" "+fl.getIsNew());
                FarmerList farmerList = farmerListsExist.stream()
                        .filter(f -> f.getCufarmerID() == fl.getCufarmerID() &&
                                f.getPlotCode().equalsIgnoreCase(fl.getPlotCode()))
                        .findFirst()
                        .orElse(null);
                if (farmerList != null){
                    System.out.println("found old");
                    deletedList.add(farmerList);
                }else{
//                    System.out.println("new " + fl.getCufarmerID());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(deletedList);
        return deletedList;
    }


    private ArrayList<FarmerList> checkFarmerListAlreadyExistForproidAndAuditID(int proId, int auditId) {
        try {
            return repository.findAllByProIDAndAuditID(proId, auditId).orElse(new ArrayList<>());
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List getFarmListForProIdAndAuditId(int proId, int auditId) {
        try {
            Optional<ArrayList<FarmerList>> farmerList = repository.findAllByProIDAndAuditID(proId, auditId);
            if (farmerList.isPresent()) {
                System.out.println("size of list : "+farmerList.get().size());
                return farmerList.get();
            } else {
                List<FarmerListFinal> farmerListFinals = farmerListFinalService.getAllFarmerListByProjectIdAndAuditId(proId, auditId);
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

    public List<FarmerList> getFarmListForProIdAndAuditId2(int proId, int auditId) {
        try {
            System.out.println("getting farmerlist for " + proId + " " + auditId);
            Optional<ArrayList<FarmerList>> farmerList = repository.findAllByProIDAndAuditID(proId, auditId);
            return farmerList.orElseGet(ArrayList::new);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void deleteFromFarmerList(List<FarmerList> farmerLists) {
        try {
            repository.deleteAll(farmerLists);
            System.out.println("deleted");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
