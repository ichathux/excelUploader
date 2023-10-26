package com.controlunion.excelUploader.repository;

import com.controlunion.excelUploader.model.Crop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CropRepository extends CrudRepository<Crop, Long> {
    Optional<Crop> findByCropName(String name);
}
