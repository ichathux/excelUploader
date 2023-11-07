package com.controlunion.excelUploader.model.comp_keys;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class FarmerListID implements Serializable {
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int listid;
    private int proID;
    private int auditID;



}
