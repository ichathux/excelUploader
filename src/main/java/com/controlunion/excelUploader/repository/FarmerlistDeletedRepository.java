package com.controlunion.excelUploader.repository;

import com.controlunion.excelUploader.model.FarmerList_deleted;
import com.controlunion.excelUploader.model.comp_keys.FarmerListID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FarmerlistDeletedRepository extends JpaRepository<FarmerList_deleted, FarmerListID> {
}
