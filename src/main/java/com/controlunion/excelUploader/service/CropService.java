package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.model.Crop;
import com.controlunion.excelUploader.repository.CropRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CropService {

    private final CropRepository cropRepository;

    public void getAllCrops() {
        System.out.println(cropRepository.findAll());
    }

    public Crop getCropByName(String name) {
        log.info("checking crop " + name + " on db");
        return cropRepository.findByCropName(name).orElse(null);
    }
}
