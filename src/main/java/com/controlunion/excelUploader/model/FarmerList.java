package com.controlunion.excelUploader.model;

import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "farmerlist")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmerList {
    @Id
    private Long listid;

    @Column(name = "cufarmerID")
    private Long cuFarmerID;

    @Column(name = "proID")
    private int proID;

    @Column(name = "auditID")
    private int auditID;

    @Column(name = "cufarmerID_old")
    private Integer cuFarmerIDOld;

    @Column(name = "newcuid")
    private Integer newCuID;

    @Column(name = "unitNoEUJAS", columnDefinition = "varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL")
    private String unitNoEUJAS;

    @Column(name = "farCodeEUJAS", columnDefinition = "varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL")
    private String farCodeEUJAS;

    @Column(name = "unitNoNOP", columnDefinition = "varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL")
    private String unitNoNOP;

    @Column(name = "farCodeNOP", columnDefinition = "varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL")
    private String farCodeNOP;

    @Column(name = "farmerName", columnDefinition = "varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL")
    private String farmerName;

    @Column(name = "totalArea")
    private Double totalArea;

    @Column(name = "city", columnDefinition = "varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL")
    private String city;

    @Column(name = "gps", columnDefinition = "varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci")
    private String gps;

    @Column(name = "address", columnDefinition = "varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci")
    private String address;

    @Column(name = "dateCert")
    @Temporal(TemporalType.DATE)
    private Date dateCert;

    @Column(name = "aplyRetrospe")
    private int aplyRetrospe;

    @Column(name = "certification", columnDefinition = "varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL")
    private String certification;

    @Column(name = "fertilizer", columnDefinition = "varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci")
    private String fertilizer;

    @Column(name = "ferUseDate", columnDefinition = "varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci")
    private String ferUseDate;

    @Column(name = "dateConfersion")
    @Temporal(TemporalType.DATE)
    private Date dateConversion;

    @Column(name = "dateorganic")
    @Temporal(TemporalType.DATE)
    private Date dateOrganic;

    @Column(name = "dateorganic_nop")
    @Temporal(TemporalType.DATE)
    private Date dateOrganicNop;

    @Column(name = "inspected")
    private boolean inspected;

    @Column(name = "inspectedDate")
    @Temporal(TemporalType.DATE)
    private Date inspectedDate;

    @Column(name = "note", columnDefinition = "varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci")
    private String note;

    @Column(name = "user", columnDefinition = "varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci")
    private String user;

    @Column(name = "sysTimeStamp")
    @Temporal(TemporalType.TIMESTAMP)
    private Date sysTimeStamp;

    @Column(name = "eujas_field", columnDefinition = "varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL")
    private String eujasField;

    @Column(name = "eujas_harvest", columnDefinition = "varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL")
    private String eujasHarvest;

    @Column(name = "usda_field", columnDefinition = "varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL")
    private String usdaField;

    @Column(name = "usda_harvest", columnDefinition = "varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL")
    private String usdaHarvest;

    @Column(name = "auditorNote", columnDefinition = "varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci")
    private String auditorNote;

    @Column(name = "isNew")
    private boolean isNew;

    @Column(name = "isChange")
    private boolean isChange;

    @Column(name = "farmName", columnDefinition = "varchar(600) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci")
    private String farmName;

    @Column(name = "plotCode", columnDefinition = "varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL")
    private String plotCode;

    @Column(name = "chng_farmdata", columnDefinition = "varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci")
    private String chngFarmData;

    @Column(name = "chng_cropdata", columnDefinition = "text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci")
    private String chngCropData;

}
