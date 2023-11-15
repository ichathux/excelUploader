package com.controlunion.excelUploader.repository;

import com.controlunion.excelUploader.model.FarmerListFinal;
import com.controlunion.excelUploader.model.comp_keys.FarmerListFinalID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import javax.persistence.QueryHint;
import java.util.ArrayList;
import java.util.Optional;

@Repository
public interface FarmerListFinalRepository extends JpaRepository<FarmerListFinal, FarmerListFinalID> {

    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    Optional<ArrayList<FarmerListFinal>> findAllByProIDAndAuditID(int proId, int auditId);

    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    Optional<ArrayList<FarmerListFinal>> findAllByProIDAndAuditIDIsLessThanOrderByListid(int proId, int auditId);

    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    Optional<ArrayList<FarmerListFinal>> findAllByProID(int proId);

    void deleteByCufarmerIDAndPlotCode(int cuid, String plotCode);

    void deleteAllByProID(int proId);

    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    boolean existsByCufarmerID(int cuid);
}
