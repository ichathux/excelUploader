package com.controlunion.excelUploader.model;

import com.controlunion.excelUploader.model.comp_keys.FarmerListFinalID;
import com.controlunion.excelUploader.model.custom.FarmerCommon;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.sql.Date;
import java.util.List;

/**
 * @author : Udara Deshan <udaradeshan.ud@gmail.com>
 * @since : 11/22/2022
 **/

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity(name = "farmerlist_final")
@IdClass(FarmerListFinalID.class)
public class FarmerListFinal implements FarmerCommon {
    @Id
    private int listid;
    private int cufarmerID;
    private String plotCode;
    @Id
    private int proID;
    @Id
    private int auditID;
    private String unitNoEUJAS;
    private String farCodeEUJAS;
    private String unitNoNOP;
    private String farCodeNOP;
    private String farmerName;
    private String farmName;
    private double totalArea;
    private String city;
    private String gps;
    private String address;
    private Date dateCert;
    private int aplyRetrospe;
    private String certification;
    private String fertilizer;
    private String ferUseDate;
    private Date dateConfersion;
    private Date dateorganic;
    private Date dateorganic_nop;
    private String note;
    private String user;
    private Date sysTimeStamp;
    private String eujas_field;
    private String eujas_harvest;
    private String usda_field;
    private String usda_harvest;
    @JsonManagedReference
    @OneToMany(mappedBy="farmerListFinal",cascade = {CascadeType.ALL})
    private List<FarmerListCropFinal> farmerListCropFinalList;
}
