package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.model.*;
import com.controlunion.excelUploader.repository.FarmerlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.sl.draw.geom.GuideIf;
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
    private final FarmerListFinalService farmerListFinalService;
//    private final PlanService planService;

    @Transactional
    public ResponseEntity<String> saveFarmerList(int proId, int auditId, Iterable<FarmerList> farmerLists) {
        log.info("Start saving on db ");
        try {
            ArrayList<FarmerList> list = new ArrayList<>();
            ArrayList<FarmerList> farmerListsExist = checkFarmerListAlreadyExistForproidAndAuditID(proId, auditId);
            System.out.println("alraedy conatin " + farmerListsExist.size());
            if (!farmerListsExist.isEmpty()) {
                for (FarmerList farmerList : farmerLists) {
                    FarmerList fl = farmerListsExist.stream()
                            .filter(f -> f.getFarCodeEUJAS().equals(farmerList.getFarCodeEUJAS())
                                    && f.getPlotCode().contentEquals(farmerList.getPlotCode()))
                            .findFirst().orElse(null);

                    if (fl != null) {

                        farmerList.setListid(fl.getListid());
                        List<FarmerListCrop> farmerListCropsnew = new ArrayList<>();

                        for (FarmerListCrop c : fl.getFarmerListCropList()) {

                            FarmerListCrop fc = farmerList.getFarmerListCropList().stream()
                                    .filter(c1 -> c1.getCropID() == c.getCropID())
                                    .findFirst()
                                    .orElse(null);

                            if (fc != null) {
                                fc.setId(c.getId());
                            } else {
                                System.out.println(farmerList);
                                System.out.println("Null crops " + c.getCropID() + " " + farmerList.getCufarmerID());
                            }
                            farmerListCropsnew.add(fc);
                        }

                        farmerList.setFarmerListCropList(farmerListCropsnew);

                    } else {
                        System.out.println("no element found for " + farmerList.getCufarmerID() + " " + farmerList.getPlotCode());
                    }

                    list.add(farmerList);

                }

                System.out.println("already contain in db");
                farmerlistRepository.saveAll(list);
            } else {
                System.out.println("new farmerlist");
                farmerlistRepository.saveAll(farmerLists);
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
        farmerlistRepository.findAllByProIDAndAuditID(farmerList.getProID(), farmerList.getAuditID()).isPresent();
//        System.out.println("deleted-1");
    }

    private ArrayList<FarmerList> checkFarmerListAlreadyExistForproidAndAuditID(int proId, int auditId) {

        return farmerlistRepository.findAllByProIDAndAuditID(proId, auditId).orElse(new ArrayList<>());
    }

    private void removeDuplicateData(ArrayList<FarmerList> farmerListsOld, Iterable<FarmerList> farmerLists,
                                     int proId, int auditId) {
        System.out.println("Removing old data");
        ArrayList<FarmerList> farmerLists1 = new ArrayList<>();

        for (FarmerList farmerList : farmerLists) {
            FarmerList fl = farmerListsOld.stream()
                    .filter(f -> f.getCufarmerID() == farmerList.getCufarmerID() &&
                            f.getPlotCode().contentEquals(farmerList.getPlotCode()))
                    .findFirst()
                    .orElse(null);
            if (fl != null) {
                farmerlistRepository.delete(fl);
                farmerLists1.add(fl);
            }
        }
    }

    public List getFarmListForProIdAndAuditId(int proId, int auditId) {
        System.out.println("getting farmerlist for " + proId + " " + auditId);
        Optional<ArrayList<FarmerList>> farmerList = farmerlistRepository.findAllByProIDAndAuditID(proId, auditId);
        if (farmerList.isPresent()){
            return farmerList.get();
        }else{
            List<FarmerListFinal> farmerListFinals = farmerListFinalService.getAllFarmerListByProjectIdAndAuditId(proId, auditId);
            if (farmerListFinals != null){
                return farmerListFinals;
            }else {
                return new ArrayList();
            }

        }
//        return farmerList.orElseGet(ArrayList::new);
        //        return farmerList.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }
    public List<FarmerList> getFarmListForProIdAndAuditId2(int proId, int auditId) {
        System.out.println("getting farmerlist for " + proId + " " + auditId);
        Optional<ArrayList<FarmerList>> farmerList = farmerlistRepository.findAllByProIDAndAuditID(proId, auditId);
        return farmerList.orElseGet(ArrayList::new);
        //        return farmerList.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }


    public void deleteFromFarmerList(List<FarmerList> farmerLists) {
        try {
            farmerlistRepository.deleteAll(farmerLists);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void moveFarmerListTofarmerListFinal(){
        try {
//            deleteFarmerListFinals();
//            saveToFarmerListFinal();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
