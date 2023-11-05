package com.controlunion.excelUploader.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.sql.Date;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity(name = "projects")
public class Projects {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "proID")
    private Long id;

    @Column(name = "proCode", nullable = false)
    private String proCode;

    @Column(name = "proName", nullable = false)
    private String proName;

    @Column(name = "proAddress")
    private String proAddress;

    @Column(name = "city")
    private String city;

    @Column(name = "contactPer")
    private String contactPer;

    @Column(name = "contactNo")
    private String contactNo;

    @Column(name = "tp")
    private String tp;

    @Column(name = "fax")
    private String fax;

    @Column(name = "email")
    private String email;

    @Column(name = "activ", nullable = false)
    private boolean activ;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "systimeStamp", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", nullable = false)
    private Date systimeStamp;

    @Column(name = "countryID", nullable = false)
    private Long countryID;

    @ManyToOne()
    @JoinColumn(name = "countryID", referencedColumnName = "countryID", insertable = false, updatable = false)
    private Country country;


}
