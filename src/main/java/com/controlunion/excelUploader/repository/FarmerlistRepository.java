package com.controlunion.excelUploader.repository;

import com.controlunion.excelUploader.model.FarmerList;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FarmerlistRepository extends CrudRepository<FarmerList, Long> {
}
