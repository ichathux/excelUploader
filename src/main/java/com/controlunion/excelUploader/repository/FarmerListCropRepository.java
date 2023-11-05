package com.controlunion.excelUploader.repository;

import com.controlunion.excelUploader.model.FarmerListCrop;
import com.controlunion.excelUploader.model.comp_keys.FarmerListCropID;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FarmerListCropRepository extends CrudRepository<FarmerListCrop, FarmerListCropID> {
}
