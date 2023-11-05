package com.controlunion.excelUploader.model.comp_keys;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class FarmerListID implements Serializable {
    private int listid;
    private int proID;
    private int auditID;
}
