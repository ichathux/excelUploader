package com.controlunion.excelUploader.model;

import lombok.Data;

import javax.persistence.*;
import java.sql.Date;

@Data
@Entity(name = "plan")
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "planID")
    private Long planID;

    @Column(name = "programID")
    private int programID;

    @JoinColumn(name = "proID", referencedColumnName = "proID", insertable = false, updatable = false)
    @ManyToOne
    private Projects proID;

    @Column(name = "dateFrom")
    private Date dateFrom;

    @Column(name = "dateTo")
    private Date dateTo;

    @Column(name = "certfifier")
    private int certifier;

    @Column(name = "auditNo")
    private String auditNo;

    @Column(name = "note", length = 2000)
    private String note;

    @Column(name = "auditStatus")
    private boolean auditStatus; // Assuming 1=Complete, 0=Incomplete

    @Column(name = "uplodFarlist")
    private boolean uplodFarlist;

    @Column(name = "uploadBy")
    private String uploadBy;

    @Column(name = "uploadTimeStmap")
    private String uploadTimeStmap;

    @Column(name = "user")
    private String user;

    @Column(name = "planeDateTime")
    private String planeDateTime;

    @Column(name = "realAuditDate_from")
    private Date realAuditDateFrom;

    @Column(name = "realAuditDate_to")
    private Date realAuditDateTo;

    @Column(name = "certifyBy")
    private String certifyBy;

    @Column(name = "certified")
    private boolean certified;

}
