package com.controlunion.excelUploader.repository;

import com.controlunion.excelUploader.model.FarmerList;
import com.controlunion.excelUploader.model.comp_keys.FarmerListID;
import com.google.protobuf.OptionOrBuilder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FarmerlistRepository extends CrudRepository<FarmerList, FarmerListID> {

    Optional<FarmerList> findFirstByOrderByListidDesc();
    Optional<List<FarmerList>> findFarmerListByProIDAndAuditID(int proId, int auditId);
}
