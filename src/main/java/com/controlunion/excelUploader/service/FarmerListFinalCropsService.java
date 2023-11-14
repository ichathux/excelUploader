package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.model.FarmerListCropFinal;
import com.controlunion.excelUploader.model.FarmerListFinal;
import com.controlunion.excelUploader.repository.FarmerListCropFinalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FarmerListFinalCropsService {

    private final FarmerListCropFinalRepository repository;


    public void deleteFarmerListCropFinalByFarmerListFinal(List<FarmerListFinal> farmerListsFarmerListFinals) {
        try {
            ArrayList<FarmerListCropFinal> farmerListCropFinals = new ArrayList<>();
            farmerListsFarmerListFinals.forEach(f -> farmerListCropFinals.addAll(f.getFarmerListCropFinalList()));
            repository.deleteAll(farmerListCropFinals);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
