package com.controlunion.excelUploader.repository;

import com.controlunion.excelUploader.model.FarmerListCropFinal;
import com.controlunion.excelUploader.model.FarmerListFinal;
import com.controlunion.excelUploader.model.comp_keys.FarmerListCropFinalID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import javax.persistence.QueryHint;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public interface FarmerListCropFinalRepository extends JpaRepository<FarmerListCropFinal, FarmerListCropFinalID> {

    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    Optional<ArrayList<FarmerListCropFinal>> findAllByFarmerListFinal(FarmerListFinal farmerListFinal);
}
