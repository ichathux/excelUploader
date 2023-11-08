package com.controlunion.excelUploader.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.sql.Date;

/**
 * @author : Udara Deshan <udaradeshan.ud@gmail.com>
 * @since : 11/1/2022
 **/
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity(name = "farmerlist_crop")
public class FarmerListCrop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = true)
    private int id;
    @Column(name = "cufarmerID", nullable = true)
    private int cufarmerID;
    @Column(name = "plotCode", nullable = true)
    private String plotCode;
    @Column(name = "cropID", nullable = true)
    private int cropID;
    @Column(name = "noOfPlant", nullable = true)
    private double noOfPlant;
    @Column(name = "estiYield", nullable = true)
    private double estiYield;
    @Column(name = "realYield", nullable = true)
    private double realYield;
    @Column(name = "noOfSesons", nullable = true)
    private double noOfSesons;
}
 