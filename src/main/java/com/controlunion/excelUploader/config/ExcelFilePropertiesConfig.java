package com.controlunion.excelUploader.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@PropertySource("classpath:sample-properties.properties")
public class ExcelFilePropertiesConfig {

    @Value("#{${excel.upload.table.header}}")
    private Map<Integer, String> header1 = new HashMap<>();

    @Value("#{${excel.upload.table.subheader}}")
    private Map<Integer, String> header2 = new HashMap<>();

    @Value("#{${excel.upload.table.crop.subheader}}")
    private Map<Integer, String> header3 = new HashMap<>();

    @Value("${excel.upload.table.products.start-point}")
    private int startPoint;

    @Value("#{${excel.upload.sheet.headers}}")
    private Map<Integer, String> auditDetails = new HashMap<>();

    @Value("${excel.upload.table.products.subheaders.count}")
    private int cropsSubheadersCount;

    @Value("${excel.upload.sheet.table.startHeader}")
    private String tableStartHeader;

    @Value("${excel.upload.date-format}")
    private String dateFormat;

    public Map<Integer, String> getHeader1() {
        return header1;
    }

    public Map<Integer, String> getHeader2() {
        return header2;
    }

    public Map<Integer, String> getHeader3() {
        return header3;
    }

    public int getStartPoint() {
        return startPoint;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public Map<Integer, String> getAuditDetails() {
        return auditDetails;
    }

    public int getCropsSubheadersCount() {
        return cropsSubheadersCount;
    }

    public String getTableStartHeader() {
        return tableStartHeader;
    }
}
