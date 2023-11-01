package com.controlunion.excelUploader.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.Instant;

/**
 * @author : Udara Deshan <udaradeshan.ud@gmail.com>
 * @since : 11/1/2022
 **/
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity(name = "crop")
public class Crop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cropID", nullable = false)
    private int cropID;
    @Column(name = "cropName", nullable = false)
    private String cropName;
    @Column(name = "plantUOM", nullable = false)
    private String plantUOM;
    @Column(name = "yieldUOM", nullable = false)
    private String yieldUOM;
    @Column(name = "estiYield", nullable = false)
    private double estiYield;
    @Column(name="active", nullable = false)
    private boolean active;
    @Column(name="cropTyp",nullable = false)
    private String cropTyp;
    @Column(name="mainCropID",nullable = false)
    private int mainCropID;
    @Column(name="itemTyp",nullable = false)
    private String itemTyp;
    @Column(name="conversion", columnDefinition="Decimal(10,2) default '0.00'")
    private String conversion;
    @Column(name = "serverDateTime", nullable = false)
    private Instant serverDateTime;
    @PrePersist
    void preInsert() {
        if (this.conversion == null) {
            this.conversion = "0.00";
        }
    }
}