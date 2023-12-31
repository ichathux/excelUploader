package com.controlunion.excelUploader.model;

import com.controlunion.excelUploader.model.comp_keys.FarmerListID;
import com.controlunion.excelUploader.model.custom.FarmerCommon;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

import java.sql.Date;
import java.util.List;

import static javax.persistence.GenerationType.IDENTITY;
import static javax.persistence.GenerationType.SEQUENCE;

/**
 * @author : Udara Deshan <udaradeshan.ud@gmail.com>
 * @since : 10/31/2022
 **/

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity(name = "farmerlist")
@IdClass(FarmerListID.class)
public class FarmerList implements FarmerCommon {

    @Id
//    @GeneratedValue
    @GeneratedValue(strategy = SEQUENCE, generator = "seqGen")
    @SequenceGenerator(name = "seqGen", sequenceName = "seq", initialValue = 1)
    @Column(name = "listid", nullable = true)
    private int listid;
    @Id
    @Column(name = "proID", nullable = true)
    private int proID;
    @Id
    @Column(name = "auditID", nullable = true)
    private int auditID;
    @Column(name = "cufarmerID", nullable = true)
    private int cufarmerID;
    @Column(name = "unitNoEUJAS", nullable = true)
    private String unitNoEUJAS;
    @Column(name = "farCodeEUJAS", nullable = true)
    private String farCodeEUJAS;
    @Column(name = "unitNoNOP", nullable = true)
    private String unitNoNOP;
    @Column(name = "farCodeNOP", nullable = true)
    private String farCodeNOP;
    @Column(name = "farmerName", nullable = true)
    private String farmerName;
    @Column(name = "totalArea", precision = 18,scale = 3,nullable = true)
    private double totalArea;
    @Column(name = "city", nullable = true)
    private String city;
    @Column(name = "gps", nullable = true)
    private String gps;
    @Column(name = "address", nullable = true)
    private String address;
    @Column(name = "dateCert", nullable = true)
    private Date dateCert;
    @Column(name = "aplyRetrospe", columnDefinition = "TINYINT",length = 3,nullable = true)
    private int aplyRetrospe;
    @Column(name = "certification", nullable = true)
    private String certification;
    @Column(name = "fertilizer", nullable = true)
    private String fertilizer;
    @Column(name = "ferUseDate", nullable = true)
    private String ferUseDate;
    @Column(name = "dateConfersion", nullable = true)
    private Date dateConfersion;
    @Column(name = "dateorganic", nullable = true)
    private Date dateorganic;
    @Column(name = "inspected", columnDefinition = "TINYINT", nullable = true)
    private int inspected;
    @Column(name = "inspectedDate", nullable = true)
    private Date inspectedDate;
    @Column(name = "note", nullable = true)
    private String note;
    @Column(name = "user", nullable = true)
    private String user;
    @Column(name = "sysTimeStamp", nullable = true)
    private Date sysTimeStamp;
    @Column(name = "eujas_field", nullable = true)
    private String eujas_field;
    @Column(name = "eujas_harvest", nullable = true)
    private String eujas_harvest;
    @Column(name = "usda_field", nullable = true)
    private String usda_field;
    @Column(name = "usda_harvest", nullable = true)
    private String usda_harvest;
    @Column(name = "auditorNote", nullable = true)
    private String auditorNote;
    @Column(name = "isNew", columnDefinition = "TINYINT", nullable = true)
    private int isNew;
    @Column(name = "isChange", columnDefinition = "TINYINT", nullable = true)
    private int isChange;
    @Column(name = "farmName", nullable = true)
    private String farmName;
    @Column(name = "plotCode", nullable = true)
    private String plotCode;
//    @JsonIgnore
    @Column(name = "chng_farmdata",columnDefinition = "varchar(2000)", nullable = true)
    private String chngFarmdata;
//    @JsonIgnore
    @Column(name = "chng_cropdata",columnDefinition = "varchar(2000)", nullable = true)
    private String chngCropdata;
    @JsonManagedReference
    @OneToMany(cascade = {CascadeType.ALL},fetch = FetchType.LAZY)
    @JoinColumn(name="listid",referencedColumnName = "listid", nullable = false)
    @JoinColumn(name="proID",referencedColumnName = "proID", nullable = false)
    @JoinColumn(name="auditID",referencedColumnName = "auditID", nullable = false)
    private List<FarmerListCrop> farmerListCropList;

}