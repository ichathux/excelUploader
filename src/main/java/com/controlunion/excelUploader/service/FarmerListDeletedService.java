package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.mapper.FarmerlistCropFinalMapper;
import com.controlunion.excelUploader.mapper.FarmerlistCropMapper;
import com.controlunion.excelUploader.mapper.FarmerlistFinalMapper;
import com.controlunion.excelUploader.mapper.FarmerlistMapper;
import com.controlunion.excelUploader.model.*;
import com.controlunion.excelUploader.repository.FarmerListCropFinalRepository;
import com.controlunion.excelUploader.repository.FarmerlistDeletedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FarmerListDeletedService {

    private final FarmerlistDeletedRepository farmerlistDeletedRepository;
    private final FarmerListFinalService farmerListFinalService;

//    public void addDataToFarmListDeleted(ArrayList<FarmerListFinal> values,
//                                         int auditID) {
//        try {
//
//            System.out.println(":Setting audit id : " + auditID);
//            Timestamp currentDateTime = new Timestamp(System.currentTimeMillis());
//            List<FarmerList_deleted> farmerList_deleteds =
//                    values
//                            .stream()
//                            .map(
//                                    (f) -> {
//                                        FarmerList_deleted farmerList_deleted = FarmerlistFinalMapper.INSTANCE.farmerListFinalToFarmerListDeleted(f);
//                                        farmerList_deleted.setAuditID(auditID);
//                                        farmerList_deleted.setUser("isuru");
//                                        farmerList_deleted.setSysTimeStamp(new Date(currentDateTime.getTime()));
//                                        List<FarmerListCrop_deleted> farmerListCropFinals =
//                                                f.getFarmerListCropFinalList()
//                                                        .stream()
//                                                        .map(FarmerlistCropFinalMapper.INSTANCE::farmerListCropFinalToFarmerListCropDeleted)
//                                                        .collect(Collectors.toList());
//                                        farmerList_deleted.setFarmerListCropList(farmerListCropFinals);
//                                        return farmerList_deleted;
//                                    })
//                            .collect(Collectors.toList());
//            farmerlistDeletedRepository.saveAll(farmerList_deleteds);
//            farmerListFinalService.deleteFarmerListFinals(values);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public void addDataToFarmListDeleted(ArrayList<FarmerList> farmerLists,
                                         int auditID) {
        try {


            ArrayList<FarmerList_deleted> farmerList_deleteds = farmerLists.stream()
                    .filter(f -> f.getIsChange() == 3)
                    .map(f -> {
                        FarmerList_deleted farmerList_deleted = FarmerlistMapper.INSTANCE.farmerListToFarmerListDelete(f);
                        ArrayList<FarmerListCrop_deleted> farmerListCrop_deleteds =
                                f.getFarmerListCropList().stream()
                                        .map(FarmerlistCropMapper.INSTANCE::farmerListCropToFarmerListCropDeleted)
                                        .collect(Collectors.toCollection(ArrayList::new));
                        farmerList_deleted.setFarmerListCropList(farmerListCrop_deleteds);
                        return farmerList_deleted;
                    })
                    .collect(Collectors.toCollection(ArrayList::new));

            for (FarmerList_deleted farmerList_deleted : farmerList_deleteds) {
//                System.out.println("deleteing " + farmerList_deleted);
                farmerListFinalService.deleteByCufarmerIDAndPlotCode(farmerList_deleted.getCufarmerID(), farmerList_deleted.getPlotCode());
            }

            farmerlistDeletedRepository.saveAll(farmerList_deleteds);
//            farmerListFinalService.deleteFarmerListFinals(values);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ArrayList<FarmerList_deleted> getAllByProIdAndAuditId(int proID, int auditId) {
        log.info(getClass().getName() + ".getAllByProIdAndAuditId proID : " + proID + " auditID : " + auditId);
        try {
            return farmerlistDeletedRepository
                    .findAllByProIDAndAuditID(proID, auditId)
                    .orElse(new ArrayList<>());
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
