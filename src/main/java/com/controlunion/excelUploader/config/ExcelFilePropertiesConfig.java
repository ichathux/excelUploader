package com.controlunion.excelUploader.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.HashMap;
import java.util.Map;

@Configuration
@PropertySource("classpath:sample-properties.properties")
public class ExcelFilePropertiesConfig {

    @Value("#{${excel.upload.table.1.header}}")
    private Map<String, String> header1 = new HashMap<>();
    @Value("#{${excel.upload.table.2.header}}")
    private Map<String, String> header2 = new HashMap<>();
    @Value("#{${excel.upload.table.3.header}}")
    private Map<Integer, String> header3 = new HashMap<>();
    @Value("${excel.upload.table.products.start-point}")
    private int startPoint;

    public Map<String, String> getHeader1() {
        return header1;
    }

    public Map<String, String> getHeader2() {
        return header2;
    }

    public Map<Integer, String> getHeader3() {
        return header3;
    }

    public int getStartPoint() {
        return startPoint;
    }
}
