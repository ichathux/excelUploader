package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.model.FarmerListCrop;
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

    public void deleteFarmerListCropFinalByProId() {
        try {
//            repository.deleteAllById();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteFarmerListCropFinalByFarmerListFinal(List<FarmerListFinal> farmerListsFarmerListFinals) {
        try {
            ArrayList<FarmerListCropFinalID> list = new ArrayList<>();
            for (FarmerListFinal farmerListFinal : farmerListsFarmerListFinals) {

                List<FarmerListCropFinalID> farmerListCropFinalIDS = farmerListFinal.getFarmerListCropFinalList().stream()
                        .map(f -> {
                            FarmerListCropFinalID fid = new FarmerListCropFinalID();
                            fid.setCropID(f.getCropID());
                            fid.setCufarmerID(f.getCufarmerID());
                            fid.setPlotCode(f.getPlotCode());
                            list.add(fid);
                            return fid;
                        }).collect(Collectors.toList());

            }
            System.out.println("fid list " + list);
            repository.deleteAllById(list);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
