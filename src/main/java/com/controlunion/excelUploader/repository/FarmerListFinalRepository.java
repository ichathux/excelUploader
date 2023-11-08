package com.controlunion.excelUploader.repository;

import com.controlunion.excelUploader.model.FarmerListCropFinal;
import com.controlunion.excelUploader.model.FarmerListFinal;
import com.controlunion.excelUploader.model.comp_keys.FarmerListFinalID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FarmerListFinalRepository extends JpaRepository<FarmerListFinal, FarmerListFinalID> {

    Optional<List<FarmerListFinal>> findAllByFarCodeEUJASAndProIDAndAuditID(String farmerCode, int proId, int auditID);
    Optional<List<FarmerListFinal>> findAllByFarCodeEUJASAndProID(String farmerCode, int proId);
    Optional<List<FarmerListFinal>> findAllByCufarmerIDAndProID(int cuFarmerId, int proId);
    Optional<List<FarmerListFinal>> findAllByProIDAndAuditID(int proId, int auditId);
//    int findTopCufarmerIDByFarCodeEUJAS(String farmerCode);
    Optional<Integer> findCufarmerIDByFarCodeEUJASAndProID(String farCodeEUJAS, int proId);
    boolean existsByFarCodeEUJAS(String farmerCode);
    boolean existsByCufarmerID(int cuid);
}
