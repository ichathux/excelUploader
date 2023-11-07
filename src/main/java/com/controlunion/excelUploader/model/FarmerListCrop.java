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
@Entity(name = "farmerlist_crop")
@IdClass(FarmerListCropID.class)
public class FarmerListCrop {

    @JsonBackReference
    @ManyToOne(cascade = CascadeType.ALL,fetch= FetchType.LAZY)
    @JoinColumn(name="listid",referencedColumnName = "listid", nullable = false)
    @JoinColumn(name="proID",referencedColumnName = "proID", nullable = false)
    @JoinColumn(name="auditID",referencedColumnName = "auditID", nullable = false)
    private FarmerList farmerList;
    @Id
    private int cropID;
    @Id
    private int cufarmerID;
    @Id
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
