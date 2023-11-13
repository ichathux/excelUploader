package com.controlunion.excelUploader.repository;

import com.controlunion.excelUploader.model.FarmerList;
import com.controlunion.excelUploader.model.comp_keys.FarmerListID;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public interface FarmerlistRepository extends CrudRepository<FarmerList, FarmerListID> {

//    Optional<List<FarmerList>> findAllByProIDAndAuditID(int proId, int auditId);
    Optional<ArrayList<FarmerList>> findAllByProIDAndAuditID(int proId, int auditId);
    void deleteAllByProIDAndAuditID(int proId, int auditId);
}
