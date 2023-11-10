package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.model.FarmerListCropFinal;
import com.controlunion.excelUploader.repository.FarmerlistDeletedRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class FarmListCropDeletedService {

    private final FarmerlistDeletedRepository farmerlistDeletedRepository;

    public void addDeletedCropsToDB(FarmerListCropFinal crop_final) {

    }
}
