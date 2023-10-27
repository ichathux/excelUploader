package com.controlunion.excelUploader.model;

import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Data
public class Crop {

    @Id
    @Column(name = "cropID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cropID;
    @Column(name = "cropName")
    public String cropName;
    @Column(name = "plantUOM")
    private String plantUOM;
    @Column(name = "yieldUOM")
    private String yieldUOM;
    @Column(name = "estiYield")
    private Double estiYield;
    @Column(name = "active")
    private Boolean active;

    @Column(name = "cropTyp")
    private Character cropTyp;
    @Column(name = "mainCropID")
    private Long mainCropID = 0L;
    @Column(name = "itemTyp")
    private Character itemTyp;
    @Column(name = "conversion")
    private Double conversion = 0.0;
    @Column(name = "serverDateTime")
    private Timestamp serverDateTime;
}
