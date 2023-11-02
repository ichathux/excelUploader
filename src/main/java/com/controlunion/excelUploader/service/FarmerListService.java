package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.model.FarmerList;
import com.controlunion.excelUploader.repository.FarmerlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FarmerListService {

    private final FarmerlistRepository farmerlistRepository;


    public String generateCuidForFarmer(){
        return "";
    }

    private void saveFarmerListOnDB(List<FarmerList> farmerLists) {
        farmerlistRepository.saveAll(farmerLists);
    }
}
