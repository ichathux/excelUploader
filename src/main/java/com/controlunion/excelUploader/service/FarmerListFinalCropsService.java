package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.model.FarmerListCrop;
import com.controlunion.excelUploader.model.FarmerListCropFinal;
import com.controlunion.excelUploader.model.FarmerListFinal;
import com.controlunion.excelUploader.model.comp_keys.FarmerListCropFinalID;
import com.controlunion.excelUploader.repository.FarmerListCropFinalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FarmerListFinalCropsService {

    private final FarmerListCropFinalRepository repository;


    public void deleteFarmerListCropFinalByFarmerListFinal(List<FarmerListFinal> farmerListsFarmerListFinals) {
        try {
            ArrayList<FarmerListCropFinal>  farmerListCropFinals = new ArrayList<>();
////            FarmerListCropFinal farmerListCropFinal = new FarmerListCropFinal();
//            farmerListsFarmerListFinals.get(0).getFarmerListCropFinalList()
//            for (FarmerListFinal farmerListFinal : farmerListsFarmerListFinals) {
//                System.out.println("Deleting "+farmerListFinal.getCufarmerID());
//                repository.deleteAllByFarmerListFinal(farmerListFinal);
//            }
        farmerListsFarmerListFinals.stream().forEach(f -> farmerListCropFinals.addAll(f.getFarmerListCropFinalList()));
//            System.out.println(farmerListCropFinals);
//            repository.deleteAllById(list);
            repository.deleteAll(farmerListCropFinals);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
