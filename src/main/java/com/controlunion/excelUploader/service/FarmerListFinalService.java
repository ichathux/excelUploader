package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.mapper.FarmerlistCropMapper;
import com.controlunion.excelUploader.mapper.FarmerlistMapper;
import com.controlunion.excelUploader.model.FarmerList;
import com.controlunion.excelUploader.model.FarmerListCrop;
import com.controlunion.excelUploader.model.FarmerListCropFinal;
import com.controlunion.excelUploader.model.FarmerListFinal;
import com.controlunion.excelUploader.repository.FarmerListFinalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FarmerListFinalService {

    private final FarmerListFinalRepository repository;
    private final Random random = new Random();

    public int createCuid() {
        try{
            int min = 100_000_000;
            int max = 999_999_999;

            int randomNineDigits = random.nextInt(max - min + 1) + min;
            int cuid = randomNineDigits;
            if (checkCUIDExist(cuid)) {
                return createCuid();
            }
            return cuid;
        }catch (Exception e){
            e.printStackTrace();
            return 0;
        }

    }

    private boolean checkCUIDExist(int cuid) {
        try{
            return repository.existsByCufarmerID(cuid);
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }


    public ArrayList<FarmerListFinal> getAllFarmerListByProjectIdAndAuditId(int proId, int auditId) {
        try {
            return repository.findAllByProIDAndAuditID(proId, auditId).orElse(new ArrayList<>());
        }catch (Exception e){
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

//    public List<FarmerListFinal> getAllFarmerListByProjectId(int proId) {
//        try {
//            return farmerListFinalRepository.findAllByProID(proId).orElse(new ArrayList<>());
//        }catch (Exception e){
//            e.printStackTrace();
//            return new ArrayList<>();
//        }
//    }

    public ArrayList<FarmerListFinal> getAllFarmerListByProjectId(int proId) {
        try {
            return repository.findAllByProID(proId).orElse(new ArrayList<>());
        }catch (Exception e){
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void deleteFarmerListFinals(List<FarmerListFinal> values) {
        try {
            repository.deleteAll(values);
            repository.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<FarmerListFinal> saveToFarmerListFinal(List<FarmerList> farmerLists) {
        try {
            ArrayList<FarmerListFinal> farmerListFinals = new ArrayList<>();
            for (FarmerList farmerList : farmerLists) {
                FarmerListFinal farmerListFinal = FarmerlistMapper.INSTANCE.farmerListToFarmerListFinal(farmerList);
                List<FarmerListCrop> farmerListCrops = farmerList.getFarmerListCropList();
                List<FarmerListCropFinal> farmerListFinalCrops = farmerListCrops.stream()
                        .map(fc -> {
                            FarmerListCropFinal fcf = FarmerlistCropMapper.INSTANCE.farmerListCropToFarmerLIstCropFinal(fc);
                            fcf.setFarmerListFinal(farmerListFinal);
                            return fcf;
                        })
                        .collect(Collectors.toList());

                farmerListFinal.setFarmerListCropFinalList(farmerListFinalCrops);
                farmerListFinals.add(farmerListFinal);
            }
            return repository.saveAllAndFlush(farmerListFinals);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }

    }

    public ArrayList<FarmerListFinal> getBeforeCertifiedFarmLIstFinal(int proId, int auditId) {
        try{

            ArrayList<FarmerListFinal> farmerListFinals = repository.findAllByProIDAndAuditID(proId, auditId).orElse(null);
            log.info("getting farmerlist final for "+proId+" "+auditId+" size"+farmerListFinals.size() );
            return farmerListFinals;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public void deleteByCufarmerIDAndPlotCode(int cuid, String plotCode){
        System.out.println("deleteing for "+cuid+" plot "+plotCode);
        repository.deleteByCufarmerIDAndPlotCode(cuid, plotCode);
    }
}
