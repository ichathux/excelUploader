package com.controlunion.excelUploader.repository;

import com.controlunion.excelUploader.model.Crop;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.persistence.QueryHint;
import java.util.Optional;

@Repository
public interface CropRepository extends JpaRepository<Crop, Long> {

    @Cacheable(value = "crops")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    Optional<Crop> findTopByCropName(String cropName);

    @Cacheable(value = "crops")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    Optional<Crop> findByCropID(int cropID);


}
