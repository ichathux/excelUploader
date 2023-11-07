package com.controlunion.excelUploader.service;


import com.controlunion.excelUploader.model.FarmerListCropFinal;
import com.controlunion.excelUploader.model.FarmerListFinal;
import com.controlunion.excelUploader.repository.FarmerListCropFinalRepository;
import com.controlunion.excelUploader.repository.FarmerListFinalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FarmerListCropFinalService {

    private final FarmerListCropFinalRepository repository;

    public List<FarmerListCropFinal> findFarmerListCropFinalsForFarmerFarmerListFinal(FarmerListFinal farmerListFinal){
        return repository.findAllByFarmerListFinal(farmerListFinal).orElseThrow(() -> new NullPointerException("No crops found for farmer "+farmerListFinal.getCufarmerID()));
    }
}
