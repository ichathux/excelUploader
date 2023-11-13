package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.model.FarmerList;
import com.controlunion.excelUploader.model.FarmerListCrop;
import com.controlunion.excelUploader.repository.FarmerListCropRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FarmerlistCropService {

    private final FarmerListCropRepository repository;

    public List<FarmerListCrop> createDeleteFarmerListCrops(ArrayList<FarmerList> needToDeleteList) {
        List<FarmerListCrop> farmerListCrops = new ArrayList<>();
        System.out.println(needToDeleteList);
        needToDeleteList.forEach(f -> farmerListCrops.addAll(f.getFarmerListCropList()));
        System.out.println(farmerListCrops);
        return farmerListCrops;
    }
}
