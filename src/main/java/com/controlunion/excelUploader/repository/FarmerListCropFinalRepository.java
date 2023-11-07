package com.controlunion.excelUploader.repository;

import com.controlunion.excelUploader.model.FarmerListCropFinal;
import com.controlunion.excelUploader.model.FarmerListFinal;
import com.controlunion.excelUploader.model.comp_keys.FarmerListCropFinalID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FarmerListCropFinalRepository extends JpaRepository<FarmerListCropFinal, FarmerListCropFinalID> {

    Optional<List<FarmerListCropFinal>> findAllByFarmerListFinal(FarmerListFinal farmerListFinal);
}
