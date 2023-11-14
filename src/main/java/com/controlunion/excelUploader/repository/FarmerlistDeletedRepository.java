package com.controlunion.excelUploader.repository;

import com.controlunion.excelUploader.model.FarmerList_deleted;
import com.controlunion.excelUploader.model.comp_keys.FarmerListID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import javax.persistence.QueryHint;
import java.util.ArrayList;
import java.util.Optional;

@Repository
public interface FarmerlistDeletedRepository extends JpaRepository<FarmerList_deleted, FarmerListID> {

    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    Optional<ArrayList<FarmerList_deleted>> findAllByProIDAndAuditID(int proId, int auditId);
}
