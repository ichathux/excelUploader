package com.controlunion.excelUploader.repository;

import com.controlunion.excelUploader.model.FarmerListFinal;
import com.controlunion.excelUploader.model.comp_keys.FarmerListFinalID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FarmerListFinalRepository extends JpaRepository<FarmerListFinal, FarmerListFinalID> {

    Optional<FarmerListFinal> findTopFinalByFarCodeEUJASAndProID(String farmerCode, int proId);
//    int findTopCufarmerIDByFarCodeEUJAS(String farmerCode);
    Optional<Integer> findCufarmerIDByFarCodeEUJASAndProID(String farCodeEUJAS, int proId);

    boolean existsByFarCodeEUJAS(String farmerCode);
    boolean existsByCufarmerID(int cuid);
}
