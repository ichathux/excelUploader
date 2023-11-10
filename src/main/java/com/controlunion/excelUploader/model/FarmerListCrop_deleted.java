package com.controlunion.excelUploader.model;

import com.controlunion.excelUploader.model.comp_keys.FarmerListCropID;
import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;


@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity(name = "farmerlist_crop_deleted")
//@IdClass(FarmerListCropID.class)
public class FarmerListCrop_deleted {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = true)
    private int id;
    @Column(name = "cropID", nullable = true)
    private int cropID;
    @Column(name = "cufarmerID", nullable = true)
    private int cufarmerID;
    private String plotCode;
    @Column(name = "noOfPlant", nullable = true)
    private double noOfPlant;
    @Column(name = "estiYield", nullable = true)
    private double estiYield;
    @Column(name = "realYield", nullable = true)
    private double realYield;
    @Column(name = "noOfSesons", nullable = true)
    private double noOfSesons;

}
