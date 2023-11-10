package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.mapper.FarmerlistCropFinalMapper;
import com.controlunion.excelUploader.mapper.FarmerlistFinalMapper;
import com.controlunion.excelUploader.model.FarmerListCropFinal;
import com.controlunion.excelUploader.model.FarmerListCrop_deleted;
import com.controlunion.excelUploader.model.FarmerListFinal;
import com.controlunion.excelUploader.model.FarmerList_deleted;
import com.controlunion.excelUploader.repository.FarmerListCropFinalRepository;
import com.controlunion.excelUploader.repository.FarmerlistDeletedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FarmerListDeletedService {

    private final FarmerlistDeletedRepository farmerlistDeletedRepository;
    private final FarmerListFinalService farmerListFinalService;

    public void addDataToFarmListDeleted(Collection<FarmerListFinal> values) {
        try {
            Timestamp currentDateTime = new Timestamp(System.currentTimeMillis());
            List<FarmerList_deleted> farmerList_deleteds = values.stream().map(f -> {

                        FarmerList_deleted farmerList_deleted = FarmerlistFinalMapper.INSTANCE.farmerListFinalToFarmerListDeleted(f);
                        farmerList_deleted.setUser("isuru");
                        farmerList_deleted.setSysTimeStamp(new Date(currentDateTime.getTime()));
                        List<FarmerListCrop_deleted> farmerListCropFinals = f.getFarmerListCropFinalList().stream()
                                .map(FarmerlistCropFinalMapper.INSTANCE::farmerListCropFinalToFarmerListCropDeleted)
                                .collect(Collectors.toList());
                        farmerList_deleted.setFarmerListCropList(farmerListCropFinals);
                        return farmerList_deleted;
                    })
                    .collect(Collectors.toList());

            System.out.println(farmerList_deleteds);

            farmerlistDeletedRepository.saveAll(farmerList_deleteds);

            farmerListFinalService.deleteFarmerListFinals(values);
        } catch (Exception e) {
            e.printStackTrace();
        }

//        return farmerlistDeletedRepository.saveAll(values);
//        return null;
    }
}
